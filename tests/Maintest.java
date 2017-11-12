import LogMe.IntuiLog;

public class Maintest {

    private static IntuiLog test1;
    private static IntuiLog test2;

    public static void main(final String[] args){
        executeTest1();
        //executeTest2();
    }

    static void executeTest1() {
        test1 = new IntuiLog();
        ClassTest t = new ClassTest(1, "1");
        test1.newTrack("Track 1");
        test1.append("test", t);
        test1.closeTrackFail("fail");
        test1.newTrack("Track 2");
        test1.append("this", "cool");
        test1.closeTrackSuccess("good end");
    }

    static void executeTest2() {
        test2 = new IntuiLog("abc");
        test2.changeLevelLog(IntuiLog.LEVEL_WARNING);
        test2.newTrack("creation du test12");
        test2.newTrack("error level");
        test2.closeTrack("good end");
    }
}
