package cn.zhijie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.session")
public class SessionProperties {

    public enum Store {
        AUTO,
        MEMORY,
        REDIS,
    }

    private Store store = Store.AUTO;

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }
}
