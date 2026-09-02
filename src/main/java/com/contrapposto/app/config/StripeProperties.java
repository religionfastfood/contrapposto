package com.contrapposto.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    private String secretKey = "";
    private String webhookSecret = "";
    private Prices prices = new Prices();

    public boolean isConfigured() {
        return !secretKey.isBlank();
    }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public Prices getPrices() { return prices; }
    public void setPrices(Prices prices) { this.prices = prices; }

    public static class Prices {
        private String modelMonthly = "";
        private String modelAnnual = "";
        private String organizerMonthly = "";
        private String organizerAnnual = "";

        public String getModelMonthly() { return modelMonthly; }
        public void setModelMonthly(String modelMonthly) { this.modelMonthly = modelMonthly; }

        public String getModelAnnual() { return modelAnnual; }
        public void setModelAnnual(String modelAnnual) { this.modelAnnual = modelAnnual; }

        public String getOrganizerMonthly() { return organizerMonthly; }
        public void setOrganizerMonthly(String organizerMonthly) { this.organizerMonthly = organizerMonthly; }

        public String getOrganizerAnnual() { return organizerAnnual; }
        public void setOrganizerAnnual(String organizerAnnual) { this.organizerAnnual = organizerAnnual; }
    }
}
