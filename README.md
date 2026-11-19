# rasp-memshellDetect

本质是学习rasp的过程中，突发奇想，能不能做一个rasp监测内存马的东西？
原理大致概括一下：所有内存马都是为了执行命令，那我直接hook执行命令的最终点：`java.lang.ProcessImpl.start()`， 用ASM的visitCode方法实现hook，
执行某个命令的时候，拿到完整的调用栈与命令行，再按照`类来源 + 白名单` 决定放行还是拦截

类加载的时候，用`BrithRecorder`记录hash与classloader等特征，再对调用栈里面的每个类做对比，对比陌生的类再做进一步处理
比如业务存在`xxx.execCmd`这个方法，那就把这个方法加到白名单里面，默认就放行。
而且由于hook的是最底层的函数，所以特意看一下个版本的jdk，都是兼容的。

具体实现可看代码，参考项目
[ApricusFindEvil](https://github.com/AfterSnows/ApricusFindEvil)