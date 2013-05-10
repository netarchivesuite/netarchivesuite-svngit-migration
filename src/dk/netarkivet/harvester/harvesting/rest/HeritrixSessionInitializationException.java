package dk.netarkivet.harvester.harvesting.rest;

/**
 * Wrapping Exception for initializing errors.
 */
public class HeritrixSessionInitializationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -3359977796822943992L;

    /**
     * @param msg
     */
    public HeritrixSessionInitializationException(final String msg) {
        super(msg);
    }

    /**
     * @param msg
     * @param cause
     */
    public HeritrixSessionInitializationException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
