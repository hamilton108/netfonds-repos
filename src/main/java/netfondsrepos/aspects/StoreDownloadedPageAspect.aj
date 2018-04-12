package netfondsrepos.aspects;

import com.gargoylesoftware.htmlunit.Page;
import oahu.annotations.StoreHtmlPage;
import oahu.functional.Procedure2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public privileged aspect StoreDownloadedPageAspect {
    Logger log = LoggerFactory.getLogger("netfondsrepos.aspects.log");

    private Procedure2<Page,String> htmlPageConsumer2;
    private Consumer<Page> htmlPageConsumer;

    public Procedure2<Page, String> getHtmlPageConsumer2() {
        return htmlPageConsumer2;
    }

    public void setHtmlPageConsumer2(Procedure2<Page, String> htmlPageConsumer) {
        this.htmlPageConsumer2 = htmlPageConsumer;
    }

    public Consumer<Page> getHtmlPageConsumer() {
        return htmlPageConsumer;
    }

    public void setHtmlPageConsumer(Consumer<Page> htmlPageConsumer) {
        this.htmlPageConsumer = htmlPageConsumer;
    }

    pointcut storeHtmlPagePointcut(StoreHtmlPage storeAnnot) :
            execution(@oahu.annotations.StoreHtmlPage Page * (..)) && @annotation(storeAnnot);

    pointcut storeHtmlPagePointcutTicker(String ticker) :
            execution(@oahu.annotations.StoreHtmlPage Page * (..)) && args(ticker);

    after(String ticker) returning (Page page) :
            storeHtmlPagePointcutTicker(ticker) {
        if (htmlPageConsumer2 != null) {

            htmlPageConsumer2.apply(page,ticker);
        }
    }
    after(StoreHtmlPage storeAnnot) returning (Page page) :
            storeHtmlPagePointcut(storeAnnot) {
        if (htmlPageConsumer != null) {
            log.debug("@StoreHtmlPage {}", storeAnnot.desc());
            htmlPageConsumer.accept(page);
        }
    }

}
