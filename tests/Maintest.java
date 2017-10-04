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
        test1.logStep("creation du test1");
        test1.changeLevelLog(IntuiLog.LEVEL_INFO);
        test1.logStep("info level");
    }

    public static void executeTest2(){
        test2 = new IntuiLog("abc");
        test2.logStep("creation du test12");
        test2.changeLevelLog(IntuiLog.LEVEL_INFO);
    }
}
