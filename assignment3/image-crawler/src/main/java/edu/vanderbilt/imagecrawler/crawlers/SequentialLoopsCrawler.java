package edu.vanderbilt.imagecrawler.crawlers;

import static edu.vanderbilt.imagecrawler.utils.Crawler.Type.IMAGE;
import static edu.vanderbilt.imagecrawler.utils.Crawler.Type.PAGE;

import java.net.URL;

import edu.vanderbilt.imagecrawler.transforms.Transform;
import edu.vanderbilt.imagecrawler.utils.Crawler;
import edu.vanderbilt.imagecrawler.utils.Image;
import edu.vanderbilt.imagecrawler.utils.WebPageElement;

/**
 * This class uses Java 7 features to perform an "image crawl"
 * starting from a root Uri. Images from HTML page reachable from
 * the root Uri are downloaded from a remote web server or the
 * local file system and the results are stored in files that are
 * displayed to the user.
 * <p>
 * This implementation is entirely sequential and uses no Java 8
 * features at all.  It therefore serves as a baseline to compare all
 * the other implementation strategies.
 * <p>
 * See https://www.mkyong.com/java/jsoup-basic-web-crawler-example for
 * an overview of how to write a web crawler using jsoup.
 */
public class SequentialLoopsCrawler // Loaded via reflection
        extends ImageCrawler {

    /**
     * Recursively crawls the passed page and returns the total
     * number of processed images.
     *
     * @param pageUri The URI that's being crawled at this point
     * @param depth   The current depth of the recursive processing
     * @return The number of images processed at this depth
     */
    @Override
    protected int performCrawl(String pageUri, int depth) {
        // Throw an exception if the the stop crawl flag has been set.
        throwExceptionIfCancelled();

        log(">> Depth: " + depth + " [" + pageUri + "]");

        // Return 0 if we've reached the depth limit of the crawl.
        if (depth > mMaxDepth) {
            log("Exceeded max depth of " + mMaxDepth);
            return 0;
        }

        // Atomically check to see if we've already visited this Uri
        // and add the new uri to the hashset so we don't try to
        // revisit it again unnecessarily.
        if (!mUniqueUris.putIfAbsent(pageUri)) {
            log("Already processed " + pageUri);
            // Return 0 if we've already examined this uri.
            return 0;
        }

        // Recursively crawl all images and hyperlinks on this
        // page returning the total number of processed images.
        return crawlPage(pageUri, depth);
    }

    /**
     * Perform a crawl starting at {@code pageUri} and return the sum
     * of all images processed during the crawl.
     *
     * @param pageUri The page uri to crawl
     * @param depth   The current depth of the recursive processing
     * @return The number of processed images
     */
    protected int crawlPage(String pageUri, int depth) {
        // Get the HTML page associated with pageUri.
        Crawler.Page page = mWebPageCrawler.getPage(pageUri);

        // Return 0 if page is null.
        if (page == null)
            return 0;
        else
            // Process the contents of the page and return the number
            // of processed images.
            return processPage(page, depth);
    }

    /**
     * Use a Java 7 features stream to (1) download and process images
     * on this page via processImage(), (2) recursively crawl other
     * hyperlinks accessible from this page via performCrawl(), and
     * (3) return the sum of all images processed during the crawl.
     *
     * @param page  The page containing HTML
     * @param depth The current depth of the recursive processing
     * @return The count of the number of images processed
     */
    protected int processPage(Crawler.Page page,
                              int depth) {
        // Return a count of of all images processed on/from this
        // page.  TODO -- you fill in here replacing this statement
        // with your solution.

        // The number of images processed at this crawl depth.
        int imageCount = 0;

        // Iterate through all images and hyperlinks on this page.
        for (WebPageElement e : page.getPageElements(IMAGE, PAGE)) {
            if (e.getType() == IMAGE) {
                // Count all the hyperlinks on this page and add the
                // result to imageCount.
                imageCount += processImage(e.getURL());
            } else {
                // Recursively visit all the hyperlinks on this page
                // and add the result to imageCount.
                imageCount += performCrawl(e.getUrl(), depth + 1);
            }
        }

        // Return the number of processed images.
        return imageCount;
    }

    /**
     * Process an image by applying any transformations that have not
     * already been applied and cached.
     *
     * @param url A image url to download and process
     * @return The count of transformed images
     */
    protected int processImage(URL url) {
        // Download the image.
        Image image = getOrDownloadImage(url);

        // If image failed to download, return.
        if (image == null) {
            return 0;
        } else
            // Return the number of processed images.
            return transformImage(image);
    }

    /**
     * Applies the current set of crawler transforms on the
     * passed {@link Image} and returns a count of all successfully
     * transformed images.
     *
     * @param image The image to transform
     * @return The count of the non-null transformed images
     */
    protected int transformImage(Image image) {
        // The resulting number of processed images.
        int imageCount = 0;

        // Apply any transforms to this image that have not already
        // been previously applied and cached.
        for (Transform transform : mTransforms) {
            // Attempt to create a new cache item for this transform
            // and only apply the transform if a new cache item was
            // actually created (i.e., was not already in the cache).
            if (createNewCacheItem(image, transform)) {
                // Apply the transformation to the image.
                applyTransform(transform, image);
                // Update the transformed images count.
                imageCount++;
            }
        }

        // Return a count of the number of images.
        return imageCount;
    }
}
