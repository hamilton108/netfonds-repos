package netfondsrepos.repos;

import com.gargoylesoftware.htmlunit.Page;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import netfondsrepos.contenthandler.DefaultContentHandler;
import oahu.annotations.Cache;
import oahu.annotations.financial.ValidateDerivativePrice;
import oahu.dto.Tuple3;
import oahu.dto.Tuple;
import oahu.financial.*;
import oahu.financial.Derivative;
import oahu.financial.DerivativePrice;
import oahu.financial.StockPrice;
import oahu.financial.html.EtradeBeanFactory;
import oahu.financial.html.EtradeDownloader;
import oahu.financial.repository.EtradeRepository;
import oahu.financial.repository.StockMarketRepository;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.log4j.Logger;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import critterrepos.beans.StockPriceBean;
import critterrepos.beans.options.DerivativeBean;
import critterrepos.beans.options.DerivativePriceBean;
import static oahu.financial.Derivative.LifeCycle;

public class DefaultEtradeRepository implements
    //EtradeRepository<Tuple<String>,Tuple2<String,File>> {
    EtradeBeanFactory,
    EtradeRepository<Tuple<String>,Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>> {

    Logger log = Logger.getLogger(getClass().getPackage().getName());

    //region interface EtradeRepository
    @Override
    public Optional<DerivativePrice> findDerivativePrice(Tuple<String> optionInfo) {
        Tuple<Collection<DerivativePrice>> tmp = null;

        String stockTicker = optionInfo.first();
        String ticker = optionInfo.second();

        //Tuple2<Optional<StockPrice>,Tuple<Collection<DerivativePrice>>> 
        Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>
            result = parseHtmlFor(optionInfo.first(),null);
        /*
        if (log.isDebugEnabled()) {
            log.debug(String.format("Fetching new derivatives for %s [%s], calls: %d, puts: %d", ticker, stockTicker,
                    result.second().first().size(),
                    result.second().second().size()));
        }
        //*/
        Optional<DerivativePrice> price =
                tmp.first().stream().filter(x -> x.getDerivative().getTicker().equals(optionInfo.second())).findAny();
        if (price.isPresent()) {
            return price;
        }
        else {
            return tmp.second().stream().filter(x -> x.getDerivative().getTicker().equals(optionInfo.second())).findAny();
        }
    }

    @Override
    public Optional<StockPrice> stockPrice(String ticker) {
        //Tuple2<Optional<StockPrice>,Tuple<Collection<DerivativePrice>>> 
        Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>
            result = parseHtmlFor(ticker,null);
        return result.first();
    }

    public Optional<StockPrice> stockPrice(String ticker, File suppliedFile) {
        Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>
                result = parseHtmlFor(ticker,suppliedFile);
        return result.first();
    }

    @Override
    @ValidateDerivativePrice()
    public Collection<DerivativePrice> puts(String ticker) {
        return putsOrCalls(ticker, false, null);
    }

    public Collection<DerivativePrice> puts(String ticker, File suppliedFile) {
        return putsOrCalls(ticker, false, suppliedFile);
    }

    @Override
    @ValidateDerivativePrice()
    public Collection<DerivativePrice> calls(String ticker) {
        return putsOrCalls(ticker, true, null);
    }

    public Collection<DerivativePrice> calls(String ticker, File suppliedFile) {
        return putsOrCalls(ticker, true, suppliedFile);
    }

    @Override
    public Collection<Derivative> callPutDefs(String ticker) {
        return callPutDefs(ticker, null);
    }

    public Collection<Derivative> callPutDefs(String ticker,File suppliedFile) {
        Collection<Derivative> result = new ArrayList<>();
        Collection<DerivativePrice> _calls = putsOrCalls(ticker, true, suppliedFile);
        Collection<DerivativePrice> _puts = putsOrCalls(ticker, false, suppliedFile);

        for (DerivativePrice c : _calls) {
            result.add(c.getDerivative());
        }
        for (DerivativePrice p : _puts) {
            result.add(p.getDerivative());
        }
        return result;
    }

    // public static enum PersistentCategory { OPTION_REF, OPTION_PRICE, STOCK_PRICE, ALL};
    /*
    public void saveToPersistentDataStore(PersistentCategory category) {
    }
    public void saveToPersistentDataStore(String ticker, File suppliedFile, PersistentCategory category) {
        switch (category) {
            case OPTION_REF:
                callPutDefs(ticker,suppliedFile).stream().filter(x -> x.getLifeCycle() == LifeCycle.FROM_HTML).forEach(y -> {
                    stockMarketRepository.insertDerivative(y,null);
                });
                break;
            case OPTION_PRICE:
                break;
            case STOCK_PRICE:
                break;
            case ALL:
                throw new NotImplementedException("PersistentCategory.ALL not implemented");
        }
    }
    //*/

    @Override
    public void setDownloadDate(LocalDate downloadDate) {
        this.downloadDate = downloadDate;
    }

    //endregion interface EtradeRepository
    //region EtradeBeanFactory  
    @Override
    public void onDerivativeParsed(String optionName, String optionType, double exercisePrice, LocalDate expiry, double buy, double sell) {
        if (stockMarketRepository != null) {
            Optional<Derivative> derivative = stockMarketRepository.findDerivative(optionName);
            if (derivative.isPresent()) {
                DerivativeBean x = (DerivativeBean)derivative.get();
                x.setLifeCycle(Derivative.LifeCycle.FROM_DATABASE);
                DerivativePrice price = new DerivativePriceBean(x,buy,sell,optionCalculator);
                if (x.getOpType() == Derivative.OptionType.CALL) {
                    currentCalls.add(price);
                }
                else {
                    currentPuts.add(price);
                }
            }
            else {
                Derivative.OptionType ot = optionType.equals("American call") ? Derivative.OptionType.CALL :
                        optionType.equals("American put") ? Derivative.OptionType.PUT :
                                optionType.equals("European call") ? Derivative.OptionType.CALL :
                                        optionType.equals("European put") ? Derivative.OptionType.PUT :
                                Derivative.OptionType.UNDEF;
                if (ot == Derivative.OptionType.UNDEF) {
                    return;
                }
                DerivativeBean derivative2 = new DerivativeBean();
                derivative2.setLifeCycle(Derivative.LifeCycle.FROM_HTML);
                derivative2.setOpType(ot);
                derivative2.setTicker(optionName);
                derivative2.setX(exercisePrice);
                derivative2.setExpiry(expiry);

                DerivativePriceBean price2 = new DerivativePriceBean(derivative2,buy,sell,optionCalculator);

                if (ot == Derivative.OptionType.CALL) {
                    currentCalls.add(price2);
                }
                else {
                    currentPuts.add(price2);
                }
            }
        }
    }
    @Override
    public void onStockPriceParsed(LocalTime spotTime, double opn, double hi, double lo, double close, int volume) {
        if (stockMarketRepository != null) {
            log.info(String.format("Download date: %s, spotTime: %s, opn: %.2f, hi: %.2f, lo: %.2f, close: %.2f",
                   downloadDate,spotTime,opn,hi,lo,close));
            Stock stock = stockMarketRepository.findStock(currentTicker);
            StockPriceBean tmp = new StockPriceBean(downloadDate, spotTime, opn, hi, lo, close, volume);
            tmp.setStock(stock);
            currentStock = Optional.of(tmp);
        }
    }

    @Override
    public void onStockPriceParsed(LocalDate spotDate,
                                   LocalTime spotTime,
                                   double opn,
                                   double hi,
                                   double lo,
                                   double close,
                                   int volume) {
        if (stockMarketRepository != null) {
            log.info(String.format("xDownload date: %s, spotTime: %s, opn: %.2f, hi: %.2f, lo: %.2f, close: %.2f",
                    spotDate,spotTime,opn,hi,lo,close));
            Stock stock = stockMarketRepository.findStock(currentTicker);
            StockPriceBean tmp = new StockPriceBean(spotDate, spotTime, opn, hi, lo, close, volume);
            tmp.setStock(stock);
            currentStock = Optional.of(tmp);
        }

    }
    //endregion EtradeBeanFactory  

    //region Private Methods
    private Collection<DerivativePrice> putsOrCalls(String ticker, boolean isCalls, File suppliedFile) {
        //Tuple<Collection<DerivativePrice>> derivatives = null;
        //Tuple2<Optional<StockPrice>,Tuple<Collection<DerivativePrice>>> 
        Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>
            result = parseHtmlFor(ticker,suppliedFile);
        /*
        if (log.isDebugEnabled()) {
            log.debug(String.format("Fetching %s for %s, calls: %d, puts: %d", (isCalls ? "calls" : "puts"),
                    ticker,
                    derivatives.first().size(),
                    derivatives.second().size()));
        }
        //*/
        return isCalls ? result.second() : result.third();
    }

    @Cache(id=1)
    @Override
    public Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>
    parseHtmlFor(String ticker,File suppliedFile) {

        Parser p = new Parser();
        p.setContentHandler(contentHandler);

        InputStream content = null;
        try {
            content = suppliedFile == null ?
                    downloader.downloadDerivatives(ticker).getWebResponse().getContentAsStream() :
                    new FileInputStream(suppliedFile);


            currentTicker = ticker;


            currentCalls = new ArrayList<>();
            currentPuts = new ArrayList<>();
            currentStock = Optional.empty();

            p.parse(new InputSource(content));

            currentStock.ifPresent(x -> {
                Consumer<DerivativePrice> curPost = postProcessor(x);
                for (DerivativePrice call : currentCalls) {
                    curPost.accept(call);
                }
                for (DerivativePrice put : currentPuts) {
                    curPost.accept(put);
                }
            });

            return new Tuple3<>(currentStock, currentCalls, currentPuts);

        } catch (IOException | SAXException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void invalidateCache() {

    }

    private Consumer<DerivativePrice> postProcessor(StockPrice p) {
        return (d) -> {
            DerivativePriceBean callx = (DerivativePriceBean)d;
            callx.setStockPrice(p);
            DerivativeBean dbx = (DerivativeBean)callx.getDerivative();
            dbx.setStock(p.getStock());
        };
    }
    //endregion Private Methods

    //region Properties
    private LocalDate downloadDate;
    private Collection<DerivativePrice> currentCalls = null;
    private Collection<DerivativePrice> currentPuts = null;
    private Optional<StockPrice> currentStock = Optional.empty();
    private String currentTicker = null;

    private StockMarketRepository stockMarketRepository;
    private EtradeDownloader<Page, Serializable> downloader;
    private DefaultContentHandler contentHandler;
    private OptionCalculator optionCalculator;

    public void setContentHandler(DefaultContentHandler contentHandler) {
        this.contentHandler = contentHandler;
        this.contentHandler.setFactory(this);
    }

    public void setDownloader(EtradeDownloader<Page, Serializable> downloader) {
        this.downloader = downloader;
    }

    public void setStockMarketRepository(StockMarketRepository stockMarketRepository) {
        this.stockMarketRepository = stockMarketRepository;
    }
    public void setOptionCalculator(OptionCalculator optionCalculator) {
        this.optionCalculator = optionCalculator;
    }
    //endregion Properties
}
