import LogMe.IntuiLog;

public class Maintest {

    public static IntuiLog test1;
    public static IntuiLog test2;

    public static void main(final String[] args){
        executeTest1();
        executeTest2();
    }

    public static void executeTest1(){
        test1 = new IntuiLog();
        ClassTest t = new ClassTest(1, "1");
        test1.logStep("creation du test1");
        test1.trace("test", t);
        test1.changeLevelLog(IntuiLog.LEVEL_ERROR);
        test1.logStep("error level");
    }

    public static void executeTest2(){
        test2 = new IntuiLog("abc");
        test2.changeLevelLog(IntuiLog.LEVEL_WARNING);
        test2.logStep("creation du test12");
    }
}
