# AntiAudioWaveView
## 实时显示手机麦克风录音的时域图

按照惯例是先上效果图的，可能稍微熟悉一点的朋友能够看出来这是公孙老师家的手机，而且是最垃圾的那一款，已经能够达到这个效果，那么其他的手机可能不会存在多大的问题。

![avatar](/Gif_20180428_142040.gif)

## 简单的说明

### 布局
```
    <dog.abcd.antilib.widget.AntiAudioWaveView
        android:id="@+id/antiAudioWaveView"
        android:layout_width="match_parent"
        android:layout_height="180dp"
        app:accuracy="1"
        app:pointTotal="80000"
        app:readCount="192"
        app:waveColor="@color/colorAccent"
        app:waveWidth="1px"/>
```
>参数说明：
>- accuracy:精确度，这个值在遍历short[]时i+=accuracy
>- pointTotal:控件一共显示的音频长度，如果录制时sampleRate=16000，并且这个值设置为16000，那么控件显示的音频长度为1s
>- readCount:控件每一帧绘制的音频长度，如果sampleRate=16000,并且这个值设置为1600，那么每100ms绘制一次
>- waveColor:线条颜色
>- waveWidth:线条宽度，1px较为合适，线条粗细会影响绘制性能
### 使用
通过putAudioData将音频数据放进缓存区
分别在onResume和onPause调用startShow()和stopShow()方法
在stopShow()调用之前应该停止putAudioData方法的调用
## Demo
已上传到GITHUB
# AntiAudioWaveSurfaceView
使用SurfaceView的实现版本，动画比View版本的会流畅得多，推荐使用这一个，参数都是差不多的，各位可以从代码中查看具体参数。