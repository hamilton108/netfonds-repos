package netfondsrepos.repos;

import com.gargoylesoftware.htmlunit.Page;
import critterrepos.beans.options.DerivativePriceBean;
import oahu.dto.Tuple;
import oahu.dto.Tuple3;
import oahu.financial.Derivative;
import oahu.financial.DerivativePrice;
import oahu.financial.StockPrice;
import oahu.financial.html.EtradeDownloader;
import oahu.financial.repository.EtradeRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;

public class EtradeRepository2 implements
        EtradeRepository<Tuple<String>,
                         Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>> {

    private EtradeDownloader<Page, Serializable> downloader;

    private Map<String,Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>>
    stoxPutsCalls = null;

    //region interface EtradeRepository
    @Override
    public Optional<DerivativePrice> findDerivativePrice(Tuple<String> optionInfo) {
        return Optional.empty();
    }

    @Override
    public Optional<StockPrice> stockPrice(String ticker) {
        return Optional.empty();
    }

    @Override
    public Collection<DerivativePrice> puts(String ticker) {
        return null;
    }

    @Override
    public Collection<DerivativePrice> calls(String ticker) {
        return null;
    }

    @Override
    public Collection<Derivative> callPutDefs(String ticker) {
        return null;
    }

    @Override
    public void setDownloadDate(LocalDate localDate) {

    }

    @Override
    public Tuple3<Optional<StockPrice>, Collection<DerivativePrice>, Collection<DerivativePrice>>
    parseHtmlFor(String ticker, File suppliedFile) {
        try {
            if (stoxPutsCalls == null) {
                stoxPutsCalls = new HashMap<>();
            }
            Tuple3<Optional<StockPrice>, Collection<DerivativePrice>, Collection<DerivativePrice>>
                    result = stoxPutsCalls.get(ticker);
            if (result == null) {
                Page page = downloader.downloadDerivatives(ticker);
                Document doc = Jsoup.parse(page.getWebResponse().getContentAsString());
                Element top = doc.getElementsByClass("com topmargin").first();
                //List<Element> trs = top.getElementsByTag("tr");
                List<Element> rawCalls = top.getElementsContainingOwnText("American call");
                List<Element> rawPuts = top.getElementsContainingOwnText("American put");

                Collection<DerivativePrice> calls = new ArrayList<>();
                for (Element el : rawCalls) {
                    DerivativePrice call = processElement(el.parent());
                    if (call != null) {
                        calls.add(call);
                    }
                }
                result = new Tuple3<>(Optional.empty(), calls, null);
            }
            return result;
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    private DerivativePrice processElement(Element el) {
        int sz = el.childNodeSize();
        if (sz < 15) {
            return null;
        }
        Element buyEl = el.child(4);
        Element sellEl = el.child(5);

        try {
            double buy = Double.parseDouble(buyEl.text());
            double sell = Double.parseDouble(sellEl.text());
            DerivativePrice bean = new DerivativePriceBean(null, buy, sell, null);
            return bean;
        }
        catch (NumberFormatException ex) {
            return null;
        }
    }
    //endregion

    //region Private Methods

    /*
    private Collection<DerivativePrice> getDerivatives() {
        if (derivativePrices == null) {
            derivativePrices = new ArrayList<>();
            try {
                Page page = downloader.downloadDerivatives();
                String[] splits = splitContents(page);
                int counter = 0;
                for (String s : splits) {
                    if (counter > 0) {
                        derivativePrices.add(fromStringRow(s));
                    }
                    ++counter;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return derivativePrices;
    }
    public String[] splitContents(Page page) {
        String content = page.getWebResponse().getContentAsString();
        return content.split("\\n");
    }
    public DerivativePrice fromStringRow(String row) {
        return null;
    }
    //*/
    //endregion

    //region Properties
    public void setDownloader(EtradeDownloader<Page, Serializable> downloader) {
        this.downloader = downloader;
    }
    //endregion Properties
}
