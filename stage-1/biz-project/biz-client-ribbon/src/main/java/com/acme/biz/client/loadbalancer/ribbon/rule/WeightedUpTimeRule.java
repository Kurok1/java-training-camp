package com.acme.biz.client.loadbalancer.ribbon.rule;

import com.netflix.appinfo.LeaseInfo;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * TODO
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class WeightedUpTimeRule implements IRule {

    private ILoadBalancer loadBalancer = null;
    private final UpTimeWeightStrategy strategy;

    private final Random random = new Random();

    public WeightedUpTimeRule() {
        this(new DefaultUpTimeWeightStrategy());
    }

    public WeightedUpTimeRule(UpTimeWeightStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public Server choose(Object key) {
        if (loadBalancer == null)
            return null;
        final long current = System.currentTimeMillis() / 1000;
        final List<Server> accessibleServer = getLoadBalancer().getReachableServers();
        if (CollectionUtils.isEmpty(accessibleServer))
            return null;

        List<ServerWeightHolder> serverWeightHolders = accessibleServer.stream()
                .map(server -> {
                    Duration duration = getDuration(server, current);
                    double weight = this.strategy.getWeight(duration);
                    return new ServerWeightHolder(server, weight);
                })
                .sorted()
                .collect(Collectors.toList());

        boolean isSameWeight = true;
        double totalWeight = serverWeightHolders.stream().mapToDouble(ServerWeightHolder::getWeight).sum();

        for (int i = 0; i < serverWeightHolders.size(); i++) {
            double weight = serverWeightHolders.get(i).getWeight();
            // Sum
            totalWeight += weight;
            if (isSameWeight && totalWeight != weight * (i + 1)) {
                isSameWeight = false;
            }
        }
        if (isSameWeight) {  //相同权重，取随机值即可
            int randomIndex = random.nextInt() * accessibleServer.size();
            return accessibleServer.get(randomIndex);
        }

        double maxWeight = serverWeightHolders.get(serverWeightHolders.size() - 1).getWeight();
        double randomWeight = random.nextDouble() * maxWeight;
        // pick the server index based on the randomIndex
        for (ServerWeightHolder holder : serverWeightHolders) {
            final double weight = holder.getWeight();
            if (weight >= randomWeight)
                return holder.getServer();
        }

        throw new RuntimeException("no service found");
    }

    @Override
    public void setLoadBalancer(ILoadBalancer lb) {
        this.loadBalancer = lb;
    }

    @Override
    public ILoadBalancer getLoadBalancer() {
        return this.loadBalancer;
    }

    protected long getUpTimeMills(DiscoveryEnabledServer server) {
        LeaseInfo leaseInfo = server.getInstanceInfo().getLeaseInfo();
        return leaseInfo.getServiceUpTimestamp();
    };

    /**
     * 获取uptime到当前时间的距离
     * @param server 服务实例
     * @param currentTimeSeconds 系统当前时间
     * @return Duration
     */
    protected Duration getDuration(Server server, long currentTimeSeconds) {
        if (server instanceof DiscoveryEnabledServer) {
            DiscoveryEnabledServer discoveryEnabledServer = (DiscoveryEnabledServer) server;
            long uptime = getUpTimeMills(discoveryEnabledServer);
            uptime = uptime / 1000;//转到秒
            return Duration.ofSeconds(currentTimeSeconds - uptime);
        } else return Duration.ZERO;
    }

    static class ServerWeightHolder implements Comparable<ServerWeightHolder> {
        private final Server server;
        private final double weight;

        public ServerWeightHolder(Server server, double weight) {
            this.server = server;
            this.weight = weight;
        }

        public Server getServer() {
            return server;
        }

        public double getWeight() {
            return weight;
        }

        @Override
        public int compareTo(ServerWeightHolder o) {
            if (o == this)
                return 0;
            return Double.compare(this.weight, o.getWeight());
        }
    }

}
