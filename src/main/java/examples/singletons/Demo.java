package examples.singletons;

/**
 * Runs only the three correct implementations.
 */
public final class Demo {

    private Demo() {
    }

    public static void main(String[] args) {
        System.out.println("CAS, at most two candidates: "
                + BoundedCasSingleton.getInstance().getValue());
        System.out.println("Holder, one instance: "
                + HolderSingleton.getInstance().getValue());
        System.out.println("Double-checked locking with volatile: "
                + DoubleCheckedSingleton.getInstance().getValue());
        System.out.println("The unsafe example without volatile is not used.");
    }
}
