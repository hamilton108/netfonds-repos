package netfondsrepos.repos;

import com.gargoylesoftware.htmlunit.Page;
import oahu.dto.Tuple;
import oahu.dto.Tuple3;
import oahu.financial.Derivative;
import oahu.financial.DerivativePrice;
import oahu.financial.StockPrice;
import oahu.financial.html.EtradeDownloader;
import oahu.financial.repository.EtradeRepository;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class EtradeRepository2 implements
        EtradeRepository<Tuple<String>,
                         Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>> {

    private EtradeDownloader<Page, Serializable> downloader;

    private Collection<DerivativePrice> derivativePrices;

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
        return null;
    }
    //endregion

    //region Private Methods
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
    //endregion

    //region Properties
    public void setDownloader(EtradeDownloader<Page, Serializable> downloader) {
        this.downloader = downloader;
    }
    //endregion Properties
}
