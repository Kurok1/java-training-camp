# ShardingSphere 5.x 与 JTA （分布式）事务的整合

### 1.事物管理器

org.apache.shardingsphere.transaction.spi.ShardingSphereTransactionManager

```java
public interface ShardingSphereTransactionManager extends AutoCloseable {
    
    void init(Map<String, DatabaseType> databaseTypes, Map<String, DataSource> dataSources, String providerType);

    TransactionType getTransactionType();

    boolean isInTransaction();

    Connection getConnection(String databaseName, String dataSourceName) throws SQLException;

    void begin();

    void begin(int timeout);

    void commit(boolean rollbackOnly);

    void rollback();

    default boolean containsProviderType(String providerType) {
        return true;
    }
}
```

* init 事物管理器的初始化方法，其参数来源于外部配置传递的数据库类型和相应的数据源
* getTransactionType 当前事物管理器所支持的事物类型，LOCAL，XA，BASE
* isInTransaction 判断当前线程是否处于事物之中
* getConnection 获取当前事物绑定的数据库链接
* begin 开启事物
* begin(timeout) 给定超时时间下开启事物
* commit 提交事物 rollbackOnly的含义不知道
* containsProviderType 是否包含给定的提供厂商（如Atomikos）

### 2.与数据库连接的桥接

org.apache.shardingsphere.transaction.ConnectionTransaction

内部维护了一个`ShardingSphereTransactionManager`，进行事物操作委派

```java
public final class ConnectionTransaction {
    
    @Getter
    private final TransactionType transactionType;
    
    private final String databaseName;
    
    @Setter
    @Getter
    private volatile boolean rollbackOnly;
    
    private final ShardingSphereTransactionManager transactionManager;
    
    public ConnectionTransaction(final String databaseName, final TransactionRule rule) {
        this(databaseName, rule.getDefaultType(), rule);
    }
    
    public ConnectionTransaction(final String databaseName, final TransactionType transactionType, final TransactionRule rule) {
        this.databaseName = databaseName;
        this.transactionType = transactionType;
        transactionManager = rule.getResource().getTransactionManager(transactionType);
        TransactionTypeHolder.set(transactionType);
    }

    public void begin() {
        transactionManager.begin();
    }

    public void commit() {
        transactionManager.commit(rollbackOnly);
    }

    public void rollback() {
        transactionManager.rollback();
    }
}
```

### 3.JTA整合

ShardingSphere支持XA，BASE等分布式事务，其中XA事务由JTA进行实现

XA事物处理继承自ShardingSphereTransactionManager

org.apache.shardingsphere.transaction.xa.XAShardingSphereTransactionManager

```java
public final class XAShardingSphereTransactionManager implements ShardingSphereTransactionManager {
    
    private final Map<String, XATransactionDataSource> cachedDataSources = new HashMap<>();
    
    private XATransactionManagerProvider xaTransactionManagerProvider;
}
```

#### 3.1 XA数据源

org.apache.shardingsphere.transaction.xa.jta.datasource.XATransactionDataSource

主要提供事物与链接的关系绑定

```java
public final class XATransactionDataSource implements AutoCloseable {
    
    private static final Set<String> CONTAINER_DATASOURCE_NAMES = new HashSet<>(Arrays.asList("AtomikosDataSourceBean", "BasicManagedDataSource"));
    
    private final ThreadLocal<Map<Transaction, Connection>> enlistedTransactions = ThreadLocal.withInitial(HashMap::new);
    
    private final DatabaseType databaseType;
    
    private final String resourceName;
    
    private final DataSource dataSource;
    
    private XADataSource xaDataSource;
}
```

还提供获取连接操作

```java
    public Connection getConnection() throws SQLException, SystemException, RollbackException {
        if (CONTAINER_DATASOURCE_NAMES.contains(dataSource.getClass().getSimpleName())) {
            return dataSource.getConnection();
        }
        Transaction transaction = xaTransactionManagerProvider.getTransactionManager().getTransaction();
        if (!enlistedTransactions.get().containsKey(transaction)) {
            Connection connection = dataSource.getConnection();
            XAConnection xaConnection = TypedSPILoader.getService(XAConnectionWrapper.class, databaseType.getType()).wrap(xaDataSource, connection);
            transaction.enlistResource(new SingleXAResource(resourceName, xaConnection.getXAResource()));
            transaction.registerSynchronization(new Synchronization() {
                
                @Override
                public void beforeCompletion() {
                    enlistedTransactions.get().remove(transaction);
                }
                
                @Override
                public void afterCompletion(final int status) {
                    enlistedTransactions.get().clear();
                }
            });
            enlistedTransactions.get().put(transaction, connection);
        }
        return enlistedTransactions.get().get(transaction);
    }
```

1. 如果数据源来源是第三方容器，则直接获取链接，比如Atomikos

2. 获取当前事物

   2.1 如果当前事物已经入队了，则从本地map中取出

   2.2 如果当前事物没有入队，则从数据源中提取当前数据库链接，利用SPI封装XAConnection. 注册`Synchronization`并加入当前本地map

#### 3.2 XA事物管理器提供方SPI

org.apache.shardingsphere.transaction.xa.spi.XATransactionManagerProvider

```java
public interface XATransactionManagerProvider extends TypedSPI, AutoCloseable {
    
    void init();
    
    void registerRecoveryResource(String dataSourceName, XADataSource xaDataSource);
    
    void removeRecoveryResource(String dataSourceName, XADataSource xaDataSource);
    
    void enlistResource(SingleXAResource singleXAResource);
    
    TransactionManager getTransactionManager();
}
```

这里主要是与JTA进行整合

* enlistResource XAResource的入队操作,其中SingleXAResource是`javax.transaction.xa.XAResource`的适配实现
* getTransactionManager 返回对应的`javax.transaction.TransactionManager`

#### 3.3 事物相关操作

##### 开启事物

```java
		@Override
    public void begin() {
        xaTransactionManagerProvider.getTransactionManager().begin();
    }
    
    @Override
    public void begin(final int timeout) {
        ShardingSpherePreconditions.checkState(timeout >= 0, TransactionTimeoutException::new);
        TransactionManager transactionManager = xaTransactionManagerProvider.getTransactionManager();
        transactionManager.setTransactionTimeout(timeout);
        transactionManager.begin();
    }
```

##### 提交

```java
		@Override
    public void commit(final boolean rollbackOnly) {
        if (rollbackOnly) {
            xaTransactionManagerProvider.getTransactionManager().rollback();
        } else {
            xaTransactionManagerProvider.getTransactionManager().commit();
        }
    }
```

##### 回滚

```java
		@Override
    public void rollback() {
        xaTransactionManagerProvider.getTransactionManager().rollback();
    }
```

可以看到ShardingSphere对于XA事物相关的操作，是直接转交给`javax.transaction.TransactionManager`执行的

### 4 atomikos整合

##### 4.1 实现`XATransactionManagerProvider`

org.apache.shardingsphere.transaction.xa.atomikos.manager.AtomikosTransactionManagerProvider

```java
public final class AtomikosTransactionManagerProvider implements XATransactionManagerProvider {
    
    @Getter
    private UserTransactionManager transactionManager;
    
    private UserTransactionService userTransactionService;
    
    @Override
    public void init() {
        transactionManager = new UserTransactionManager();
        userTransactionService = new UserTransactionServiceImp();
        userTransactionService.init();
    }
    
    @Override
    public void registerRecoveryResource(final String dataSourceName, final XADataSource xaDataSource) {
        userTransactionService.registerResource(new AtomikosXARecoverableResource(dataSourceName, xaDataSource));
    }
    
    @Override
    public void removeRecoveryResource(final String dataSourceName, final XADataSource xaDataSource) {
        userTransactionService.removeResource(new AtomikosXARecoverableResource(dataSourceName, xaDataSource));
    }
    
    @SneakyThrows({SystemException.class, RollbackException.class})
    @Override
    public void enlistResource(final SingleXAResource xaResource) {
        transactionManager.getTransaction().enlistResource(xaResource);
    }
    
    @Override
    public void close() {
        userTransactionService.shutdown(true);
    }
    
    @Override
    public String getType() {
        return "Atomikos";
    }
    
    @Override
    public boolean isDefault() {
        return true;
    }
}
```

初始化阶段：

* UserTransactionManager atomikos内部对于`javax.transaction.TransactionManager`的实现
* UserTransactionService atomikos内部对应分布式事物相关操作

其他相关操作的实现都是直接委派给atomikos去执行

### 5.调用链路

#### 开启事物

org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection#setAutoCommit

​	-> org.apache.shardingsphere.transaction.ConnectionTransaction#begin

​		-> org.apache.shardingsphere.transaction.xa.XAShardingSphereTransactionManager#begin()

​			-> javax.transaction.TransactionManager#begin

注意，setAutomCommit=false时，才会创建事物，=true时则直接提交事物



#### 提交事物

org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection#commit

​	-> org.apache.shardingsphere.driver.jdbc.core.connection.ConnectionManager#commit

​		-> org.apache.shardingsphere.transaction.ConnectionTransaction#commit

​			-> org.apache.shardingsphere.transaction.xa.XAShardingSphereTransactionManager#commit

​				-> javax.transaction.TransactionManager#commit

#### 回滚事物

org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection#rollback

​	-> org.apache.shardingsphere.driver.jdbc.core.connection.ConnectionManager#rollback

​		-> org.apache.shardingsphere.transaction.ConnectionTransaction#rollback

​			-> org.apache.shardingsphere.transaction.xa.XAShardingSphereTransactionManager#rollback

​				-> javax.transaction.TransactionManager#rollback