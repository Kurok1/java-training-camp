# Spring 事务传播机制与 EJB 规范的联系

## Mandatory



### EJB相关表述

> The container must invoke an enterprise bean method whose transaction attribute is set to Mandatory in a client’s transaction context. The client is required to call with a transaction context.
>
> **•** If the client calls with a transaction context, the container performs the same steps as described in the Required case.
>
> **•** If the client calls without a transaction context, the container throws the javax.transaction.TransactionRequiredException exception 
>
> if the client is a remote client, or the javax.ejb.TransactionRequiredLocalException if the client is a local client.

要点：

	1. 如果当前客户端执行当前方法时，处于其他事务上下文中，那么就跟**Required**表现一致（加入到当前事物中）
	1. 如果当前客户端执行当前方法时，没有其他事务上下文中，容器会抛出异常(`TransactionRequiredException`|`TransactionRequiredLocalException`)



### Spring相关实现

#### 1.获取当前事物上下文-`org.springframework.transaction.support.AbstractPlatformTransactionManager#getTransaction`

```java
	public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition)
			throws TransactionException {

		// Use defaults if no transaction definition given.
		TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());

		Object transaction = doGetTransaction();
		boolean debugEnabled = logger.isDebugEnabled();

		if (isExistingTransaction(transaction)) {
			// Existing transaction found -> check propagation behavior to find out how to behave.
			return handleExistingTransaction(def, transaction, debugEnabled);
		}

		// No existing transaction found -> check propagation behavior to find out how to proceed.
		if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
			throw new IllegalTransactionStateException(
					"No existing transaction found for transaction marked with propagation 'mandatory'");
		}
		// 其余传播行为...
	}
```

通过代码可以发现，会先检查当前线程是否存在其他上下文，如果不存在，同时事物传播行为设置为`PROPAGATION_MANDATORY`，则抛出异常

#### 2.检查当前线程是否存在上下文-`org.springframework.transaction.support.AbstractPlatformTransactionManager#isExistingTransaction`

由于是jdbc事物，我们直接查看其实现`DataSourceTransactionManager`

```java
	@Override
	protected boolean isExistingTransaction(Object transaction) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
		return (txObject.hasConnectionHolder() && txObject.getConnectionHolder().isTransactionActive());
	}
```

可以看到，是通过判断**DataSourceTransactionObject**中的**ConnectionHandler**来判断当前线程是否有事物

而**ConnectionHandler**，会在当前线程任意方法开启事物时，加入到**DataSourceTransactionObject**中

```java
protected void doBegin(Object transaction, TransactionDefinition definition) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
		Connection con = null;

		try {
			if (!txObject.hasConnectionHolder() ||
					txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
				Connection newCon = obtainDataSource().getConnection();
				if (logger.isDebugEnabled()) {
					logger.debug("Acquired Connection [" + newCon + "] for JDBC transaction");
				}
        //当前事物存在标识
				txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
			}
			//其余事物属性设置，如只读，超时等。
	}
```

#### 3.对当前已存在事物的处理-`org.springframework.transaction.support.AbstractPlatformTransactionManager#handleExistingTransaction`

```java
	private TransactionStatus handleExistingTransaction(
			TransactionDefinition definition, Object transaction, boolean debugEnabled)
			throws TransactionException {

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
			throw new IllegalTransactionStateException(
					"Existing transaction found for transaction marked with propagation 'never'");
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
			//NOT_SUPPORTED处理
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
      //REQUIRES_NEW 处理
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
			//NESTED 处理
		}

		// Assumably PROPAGATION_SUPPORTS or PROPAGATION_REQUIRED.
		boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
		return prepareTransactionStatus(definition, transaction, false, newSynchronization, debugEnabled, null);
	}
```

可以看到，在当前线程存在事物的场景下，`MANDATORY`的处理和`REQUIRED`是保持一致的，即加入当前事物





## Never

### EJB相关表述

> The container invokes an enterprise bean method whose transaction attribute is set to Never without a transaction context defined by the EJB specification. The client is required to call without a transaction context.
>
> **•** If the client calls with a transaction context, the container throws the java.rmi.RemoteException exception if the client is a remote client, or the javax.ejb.EJBException if the client is a local client.
>
> **•** If the client calls without a transaction context, the container performs the same steps as described in the NotSupported case.

要点：

当传播行为设置为Never时，强制要求容器在没有事物的环境中执行方法

 	1. 如果当前客户端执行当前方法时，处于其他事务上下文中，抛出异常(`java.rmi.RemoteException`|`javax.ejb.EJBException`)
 	2. 如果当前客户端执行当前方法时，没有其他事务上下文中，正常执行，同`NOT_SUPPORTED`



### Spring相关实现

#### 1.获取当前事物上下文-`org.springframework.transaction.support.AbstractPlatformTransactionManager#getTransaction`

```java
	public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition)
			throws TransactionException {

		// Use defaults if no transaction definition given.
		TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());

		Object transaction = doGetTransaction();
		boolean debugEnabled = logger.isDebugEnabled();

		if (isExistingTransaction(transaction)) {
			// Existing transaction found -> check propagation behavior to find out how to behave.
			return handleExistingTransaction(def, transaction, debugEnabled);
		}

		// 其余传播行为的判断...
	}
```

通过代码可以发现，会先检查当前线程是否存在其他上下文，并处理当前上下文

#### 2.对当前已存在事物的处理-`org.springframework.transaction.support.AbstractPlatformTransactionManager#handleExistingTransaction`

```java
	private TransactionStatus handleExistingTransaction(
			TransactionDefinition definition, Object transaction, boolean debugEnabled)
			throws TransactionException {

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
			throw new IllegalTransactionStateException(
					"Existing transaction found for transaction marked with propagation 'never'");
		}

		//后续处理
	}
```

可以看到，当传播行为为`NEVER`时，是直接抛出异常，同时在`getTransaction`的后续处理中，针对`Never`的传播行为也不会开启事物

```java

		// No existing transaction found -> check propagation behavior to find out how to proceed.
		if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
			throw new IllegalTransactionStateException(
					"No existing transaction found for transaction marked with propagation 'mandatory'");
		}
		else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
				def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
				def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
			SuspendedResourcesHolder suspendedResources = suspend(null);
			if (debugEnabled) {
				logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);
			}
			try {
				return startTransaction(def, transaction, debugEnabled, suspendedResources);
			}
			catch (RuntimeException | Error ex) {
				resume(null, suspendedResources);
				throw ex;
			}
		}
		else {
			// Create "empty" transaction: no actual transaction, but potentially synchronization.
			if (def.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
				logger.warn("Custom isolation level specified but no actual transaction initiated; " +
						"isolation level will effectively be ignored: " + def);
			}
			boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
      //这里事物为null
			return prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null);
		}
```





## REQUIRED_NEW

### EJB相关表述

> The container must invoke an enterprise bean method whose transaction attribute is set to RequiresNew with a new transaction context.
>
> 
>
> If the client invokes the enterprise bean’s method while the client is not associated with a transaction context, the container automatically starts a new transaction before delegating a method call to the enterprise bean business method. The container automatically enlists all the resource managers accessed by the business method with the transaction. If the business method invokes other enterprise beans, the container passes the transaction context with the invocation. The container attempts to commit the transaction when the business method has completed. The container performs the commit protocol before the method result is sent to the client.
>
> 
>
> If a client calls with a transaction context, the container suspends the association of the transaction context with the current thread before starting the new transaction and invoking the business method. The container resumes the suspended transaction association after the business method and the new transac- tion have been completed.

要点：

 	1. 如果当前客户端执行当前方法时，没有处于其他事务上下文中，那么容器会手动创建一个新的事物，同时允许传播至其他方法。当业务方法执行完成后，事物自动关闭。
 	2. 如果当前客户端执行当前方法时，正好其他事务上下文中，会将当前已存在的所有事物上下文全部挂起，同时开启一个新的事物。当前方法执行完成后，恢复之前挂起的事物。



### Spring相关实现

#### 1.获取当前事物上下文-`org.springframework.transaction.support.AbstractPlatformTransactionManager#getTransaction`

```java
	public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition)
			throws TransactionException {

		// Use defaults if no transaction definition given.
		TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());

		Object transaction = doGetTransaction();
		boolean debugEnabled = logger.isDebugEnabled();

		if (isExistingTransaction(transaction)) {
			// Existing transaction found -> check propagation behavior to find out how to behave.
			return handleExistingTransaction(def, transaction, debugEnabled);
		}

		// 其余传播行为的判断...
    else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
				def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
				def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
			SuspendedResourcesHolder suspendedResources = suspend(null);
			if (debugEnabled) {
				logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);
			}
			try {
				return startTransaction(def, transaction, debugEnabled, suspendedResources);
			}
			catch (RuntimeException | Error ex) {
				resume(null, suspendedResources);
				throw ex;
			}
		}
	}
```

通过代码可以发现，会先检查当前线程是否存在其他上下文，并处理当前上下文。同时如果当前线程没有其他事物上下文，则创建一个新的事物并开启

#### 2.对当前已存在事物的处理-`org.springframework.transaction.support.AbstractPlatformTransactionManager#handleExistingTransaction`

```java
	private TransactionStatus handleExistingTransaction(
			TransactionDefinition definition, Object transaction, boolean debugEnabled)
			throws TransactionException {

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
			if (debugEnabled) {
				logger.debug("Suspending current transaction, creating new transaction with name [" +
						definition.getName() + "]");
			}
			SuspendedResourcesHolder suspendedResources = suspend(transaction);
			try {
				return startTransaction(definition, transaction, debugEnabled, suspendedResources);
			}
			catch (RuntimeException | Error beginEx) {
				resumeAfterBeginException(transaction, suspendedResources, beginEx);
				throw beginEx;
			}
		}

		//后续处理
	}
```

可以看到，当传播行为为`REQUIRES_NEW`时

1. 挂起当前事物
2. 将当前挂起的事物，作为一个**资源附件**，去开启一个新的事物

#### 3.事物的挂起-`org.springframework.transaction.support.AbstractPlatformTransactionManager#suspend`

```java
	protected final SuspendedResourcesHolder suspend(@Nullable Object transaction) throws TransactionException {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			List<TransactionSynchronization> suspendedSynchronizations = doSuspendSynchronization();
			try {
				Object suspendedResources = null;
				if (transaction != null) {
					suspendedResources = doSuspend(transaction);
				}
				String name = TransactionSynchronizationManager.getCurrentTransactionName();
				TransactionSynchronizationManager.setCurrentTransactionName(null);
				boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
				TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
				Integer isolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
				TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
				boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();
				TransactionSynchronizationManager.setActualTransactionActive(false);
				return new SuspendedResourcesHolder(
						suspendedResources, suspendedSynchronizations, name, readOnly, isolationLevel, wasActive);
			}
			catch (RuntimeException | Error ex) {
				// doSuspend failed - original transaction is still active...
				doResumeSynchronization(suspendedSynchronizations);
				throw ex;
			}
		}
		else if (transaction != null) {
			// Transaction active but no synchronization active.
			Object suspendedResources = doSuspend(transaction);
			return new SuspendedResourcesHolder(suspendedResources);
		}
		else {
			// Neither transaction nor synchronization active.
			return null;
		}
	}
```

核心逻辑在于`doSuspend`方法，针对jdbc事物，直接查看其实现`org.springframework.jdbc.datasource.DataSourceTransactionManager#doSuspend`

```java
	protected Object doSuspend(Object transaction) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
		txObject.setConnectionHolder(null);
		return TransactionSynchronizationManager.unbindResource(obtainDataSource());
	}
```

可以看到挂起时，清除掉了当前的ConnectionHolder,同时解除`TransactionSynchronizationManager`的绑定关系,此时在后续判断时，便可以认为当前线程没有事物



4.事物的恢复-`org.springframework.transaction.support.AbstractPlatformTransactionManager#cleanupAfterCompletion`

前面提到，**挂起的事物，会作为一个资源附件加入到新事物当中**

那么在业务方法执行完成后，自然而然的也会依据这个资源来恢复原有事物



```java
	private void cleanupAfterCompletion(DefaultTransactionStatus status) {
		status.setCompleted();
		if (status.isNewSynchronization()) {
			TransactionSynchronizationManager.clear();
		}
		if (status.isNewTransaction()) {
			doCleanupAfterCompletion(status.getTransaction());
		}
		if (status.getSuspendedResources() != null) {
			if (status.isDebug()) {
				logger.debug("Resuming suspended transaction after completion of inner transaction");
			}
			Object transaction = (status.hasTransaction() ? status.getTransaction() : null);
			resume(transaction, (SuspendedResourcesHolder) status.getSuspendedResources());
		}
	}
```

`suspendedResources`便是之前提及的挂起的事物

详细实现位于`org.springframework.jdbc.datasource.DataSourceTransactionManager#doResume`

```java
	@Override
	protected void doResume(@Nullable Object transaction, Object suspendedResources) {
		TransactionSynchronizationManager.bindResource(obtainDataSource(), suspendedResources);
	}
```

此时重新绑定了`TransactionSynchronizationManager`资源,后续判断时，可以根据`TransactionSynchronizationManager`绑定的资源，判断当前线程是否存在事物



Spring对于`REQUIRES_NEW`挂起的实现本质还是资源的解绑过程

