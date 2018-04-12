package netfondsrepos.downloader;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import oahu.dto.Tuple;
import oahu.financial.html.EtradeDownloader;

import java.io.IOException;
import java.util.Optional;

public class ScpDownloader implements EtradeDownloader<Page> {
    private boolean javaScriptEnabled = false;

    @Override
    public Page downloadDerivatives(String ticker) throws IOException {
        return null;
    }

    @Override
    public Page downloadIndex(String stockIndex) throws IOException {
        return null;
    }

    @Override
    public Page downloadPaperHistory(String ticker) throws IOException {
        return null;
    }

    @Override
    public Page downloadDepth(String ticker) throws IOException {
        return null;
    }

    @Override
    public Page downloadPurchases(String ticker) throws IOException {
        return null;
    }

    //region Properties
    boolean isJavaScriptEnabled() {
        return javaScriptEnabled;
    }

    public void setJavaScriptEnabled(boolean javaScriptEnabled) {
        this.javaScriptEnabled = javaScriptEnabled;
    }
    //endregion
}
