package netfondsrepos.repos;

import java.util.Collection;
import java.util.HashMap;
import oahu.dto.Tuple3;
import oahu.dto.Tuple;
import oahu.financial.StockPrice;
import oahu.financial.Derivative;
import oahu.financial.DerivativePrice;
import oahu.financial.repository.ChachedEtradeRepository;
import oahu.financial.repository.EtradeRepository;

import java.util.Optional;

public class ChachedEtradeReposImpl implements ChachedEtradeRepository<Tuple<String>> {
    private EtradeRepository repos;

    HashMap<String,Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>>
    parsedTickers = new HashMap<>();

    private
    Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>
    getCachedTicker(String stockTicker) {
        Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>
        cachedTicker = parsedTickers.get(stockTicker);
        if (cachedTicker == null) {
            cachedTicker = repos.parseHtmlFor(stockTicker,null);
            parsedTickers.put(stockTicker,cachedTicker);
        }
        return cachedTicker;
    }
    @Override
    public Optional<DerivativePrice> findDerivativePrice(Tuple<String> optionInfo) {
        String stockTicker = optionInfo.first();
        String ticker = optionInfo.second();

        Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>> cachedTicker
                = getCachedTicker(stockTicker);


        Optional<DerivativePrice> price =
                cachedTicker.second().stream().filter(x -> x.getDerivative().getTicker().equals(ticker)).findAny();
        if (price.isPresent()) {
            return price;
        }
        else {
            return cachedTicker.third().stream().filter(x -> x.getDerivative().getTicker().equals(ticker)).findAny();
        }
    }

    @Override
    public Optional<StockPrice> findSpot(String ticker) {
        Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>> cachedTicker
                = getCachedTicker(ticker);
        return cachedTicker.first();
    }

    @Override
    public void invalidateCache() {
        parsedTickers = new HashMap<>();
    }

    public void setRepos(EtradeRepository repos) {
        this.repos = repos;
    }
}
