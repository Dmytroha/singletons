package examples.singletons;

/**
 * Запускает только три корректных варианта.
 */
public final class Demo {

    private Demo() {
    }

    public static void main(String[] args) {
        System.out.println("CAS, максимум два кандидата: "
                + BoundedCasSingleton.getInstance().getValue());
        System.out.println("Holder, один экземпляр: "
                + HolderSingleton.getInstance().getValue());
        System.out.println("Двойная проверка с volatile: "
                + DoubleCheckedSingleton.getInstance().getValue());
        System.out.println("Антипример без volatile не используется.");
    }
}
