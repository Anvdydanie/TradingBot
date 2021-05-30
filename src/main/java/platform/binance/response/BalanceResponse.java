package platform.binance.response;

import lombok.Data;

import java.util.List;

@Data
public class BalanceResponse {
    private int makerCommission;
    private int takerCommission;
    private int buyerCommission;
    private int sellerCommission;
    private boolean canTrade;
    private boolean canWithdraw;
    private boolean canDeposit;
    private long updateTime;
    private String accountType;
    private List<Balance> balances;
    private String[] permissions;

    @Data
    public static class Balance {
        private String asset;
        private String free;
        private String locked;
    }
}
