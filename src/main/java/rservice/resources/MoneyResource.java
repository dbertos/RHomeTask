package rservice.resources;

import rservice.services.MoneyService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;

@Path("money")
public class MoneyResource {

    private static final MoneyService MONEY_SERVICE = new MoneyService();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("balance/{accountId}/")
    public String getAccountBalance(@PathParam("accountId") int accountId) {
        try {
            BigDecimal balance = MONEY_SERVICE.getBalance(accountId);
            return "Account with id " + accountId + " has balance " + balance;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("transfer/{senderId}/{receiverId}/{amount}")
    public String makeTransfer(@PathParam("senderId") int senderId,
                               @PathParam("receiverId") int receiverId,
                               @PathParam("amount") BigDecimal amount) {
        try {
            MONEY_SERVICE.transferMoney(senderId, receiverId, amount);
            return "Transfer from account with id " + senderId
                    + " to account with id " + receiverId
                    + " succeeded";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}
