package netfondsrepos.contenthandler;

import oahu.financial.html.EtradeBeanFactory;

import java.time.LocalDate;
import java.time.LocalTime;

public class DerivativePriceFactory implements EtradeBeanFactory {
    @Override
    public void onDerivativeParsed(String optionName,
                                   String optionType,
                                   double exercisePrice,
                                   LocalDate expiry,
                                   double buy,
                                   double sell) {

        System.out.println(
                String.format("%s: %s, %s, %s, %s, %s",
                        optionName, optionType, exercisePrice, expiry, buy, sell));
    }

    @Override
    public void onStockPriceParsed(LocalTime spotTime, double opn, double hi, double lo, double close, int volume) {

    }

    @Override
    public void onStockPriceParsed(LocalDate spotDate, LocalTime spotTime, double opn, double hi, double lo, double close, int volume) {

    }
}
