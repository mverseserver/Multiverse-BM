package id.multiverse.blackmarket.market;

import id.multiverse.blackmarket.economy.EconomyManager.CurrencyType;

public class ActiveOffer {

    private final RewardDefinition definition;
    private int remainingStock;
    private final double currentPrice;

    public ActiveOffer(RewardDefinition definition, int stock, double price) {
        this.definition = definition;
        this.remainingStock = stock;
        this.currentPrice = price;
    }

    public boolean decrementStock() {
        if (definition.getStock() == 0) return true; // unlimited
        if (remainingStock <= 0) return false;
        remainingStock--;
        return true;
    }

    public boolean isSoldOut() {
        if (definition.getStock() == 0) return false; // unlimited
        return remainingStock <= 0;
    }

    public RewardDefinition getDefinition() { return definition; }
    public int getRemainingStock() { return remainingStock; }
    public double getCurrentPrice() { return currentPrice; }
    public CurrencyType getCurrency() { return definition.getCurrency(); }
}
