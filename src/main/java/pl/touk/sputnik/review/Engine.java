package pl.touk.sputnik.review;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import pl.touk.sputnik.configuration.ConfigurationHolder;
import pl.touk.sputnik.connector.ConnectorFacade;
import pl.touk.sputnik.processor.checkstyle.CheckstyleProcessor;
import pl.touk.sputnik.processor.findbugs.FindBugsProcessor;
import pl.touk.sputnik.processor.pmd.PmdProcessor;
import pl.touk.sputnik.processor.scalastyle.ScalastyleProcessor;

import java.util.ArrayList;
import java.util.List;
import pl.touk.sputnik.configuration.GeneralOption;

@Slf4j
public class Engine {
    private static final long THOUSAND = 1000L;
    private final ConnectorFacade facade;

    public Engine(ConnectorFacade facade) {
        this.facade = facade;
    }

    public void run() {
        List<ReviewFile> reviewFiles = facade.listFiles();
        Boolean reviewTestFiles = Boolean.valueOf(ConfigurationHolder.instance().getProperty(GeneralOption.PROCESS_TEST_FILES));
        
        Review review = new Review(reviewFiles, reviewTestFiles);

        List<ReviewProcessor> processors = createProcessors();
        for (ReviewProcessor processor : processors) {
            review(review, processor);
        }
        
        int maxNumberOfComments = Integer.parseInt(ConfigurationHolder.instance().getProperty(GeneralOption.MAX_NUMBER_OF_COMMENTS));

        facade.setReview(review.toReviewInput(maxNumberOfComments));
    }

    private void review(@NotNull Review review, @NotNull ReviewProcessor processor) {
        log.info("Review started for processor {}", processor.getName());
        long start = System.currentTimeMillis();

        ReviewResult reviewResult = processor.process(review);
        log.info("Review finished for processor {}. Took {} s", processor.getName(), (System.currentTimeMillis() - start) / THOUSAND);

        if (reviewResult == null) {
            log.warn("Review for processor {} returned empty review", processor.getName());
        } else {
            log.info("Review for processor {} returned {} violations", processor.getName(), reviewResult.getViolations().size());
            review.add(processor.getName(), reviewResult);
        }
    }

    @NotNull
    private List<ReviewProcessor> createProcessors() {
        List<ReviewProcessor> processors = new ArrayList<>();
        if (Boolean.valueOf(ConfigurationHolder.instance().getProperty(GeneralOption.CHECKSTYLE_ENABLED))) {
            processors.add(new CheckstyleProcessor());
        }
        if (Boolean.valueOf(ConfigurationHolder.instance().getProperty(GeneralOption.PMD_ENABLED))) {
            processors.add(new PmdProcessor());
        }
        if (Boolean.valueOf(ConfigurationHolder.instance().getProperty(GeneralOption.FINDBUGS_ENABLED))) {
            processors.add(new FindBugsProcessor());
        }
        if (Boolean.valueOf(ConfigurationHolder.instance().getProperty(GeneralOption.SCALASTYLE_ENABLED))) {
            processors.add(new ScalastyleProcessor());
        }
        return processors;
    }
}
