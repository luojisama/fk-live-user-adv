package moe.shiro.lsposed.contentfilter.hook;

final class CurrentTarget {
    volatile String packageName = "";
    volatile String processName = "";
    volatile String currentActivity = "";
}
