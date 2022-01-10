package org.tivrfoa;

class Bar {
    private static final Logger logger = new Logger(Bar.class.getName());

    public void doIt() {
        logger.trace("doing trace");
        logger.debug("doing my job");
        logger.info("doing info");
        logger.error("doing error");
    }

    public static void main(String[] args) {
        new Bar().doIt();
    }
}