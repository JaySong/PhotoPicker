# PhotoPicker
一个属于material design风格的图片选择库

# Image Show


# 目录结构
```
PhotoPicker Library
com.jay.ui
  |--entity
    |--Photo.java
  |--widget
    |--SquareFrameLayout.java
    |--TouchImageView.java
  |--PhotoPickerActivity.java
  |--PhotoPreviewFragment.java
```
# dependencies
```
compile 'com.android.support:appcompat-v7:23.3.0'
compile 'com.android.support:recyclerview-v7:23.3.0'
compile 'com.github.bumptech.glide:glide:3.7.0'
compile 'com.android.support:design:23.3.0'
```
# user permission
```
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

# Code
```
/*调用姿势*/
Intent intent = new Intent(MainActivity.this, PhotoPickerActivity.class);
switch (v.getId()) {
    case R.id.btn1://单选
        isMultiSelect = false;
        break;
    case R.id.btn2://多选
        isMultiSelect = true;
        Bundle bundle = new Bundle();
        bundle.putBoolean(PhotoPickerActivity.IS_MULTI_SELECT, true);//设置是否支持多选
        bundle.putInt(PhotoPickerActivity.MAX_SELECT_SIZE, MAX_COUNT);//设置最大选择数量
        intent.putExtras(bundle);
        break;
}
startActivityForResult(intent,REQUEST_CODE);

/*回传接收*/
 @Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
        if (isMultiSelect) {
            //多选
            ArrayList<String> results = data.getStringArrayListExtra(PhotoPickerActivity.SELECT_RESULTS_ARRAY);
        }else{
            //单选
            String result = data.getStringExtra(PhotoPickerActivity.SELECT_RESULTS);
        }
    }
}
```

# Photo.java
图片对象JavaBean
```
public String uri;//图片原图地址
public boolean isChecked;//当前对象在列表是否为选中状态
```

# SquareFrameLayout.java
方形帧布局,主要保证列表页面显示的View为正方形;
仅仅重写了onMeasure方法,将widthMeasureSpec传给了父类heightMeasureSpec的位置
```
@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec,widthMeasureSpec);
}
```

# TouchImageView.java
from github [TouchImageView][1]

  [1]: https://github.com/MikeOrtiz/TouchImageView

# PhotoPickerActivity.java
核心类,负责加载本地所有图片并显示;
图片查询采用实现 LoaderManager.LoaderCallbacks<Cursor> 接口进行媒体库的图片查询;
图片列表的展示使用 RecyclerView 进行Grid形式排列显示

# PhotoPreviewFragment.java
图片大图预览类

# 使用注意事项
前言:本项目基本是按MD风格进行设计开发的,所以大家使用的时候请慎重考虑<风格如需要改,可考虑自己下载源码进行修改>

 - 本项目用到了Toolbar,所以请大家在配置的时候至少请设置PhotoPickerActivity的Theme为NoActionBar样式
 - 上面提到的dependencies请保证添加
 - 本项目使用的icon为SVG格式,所以请保证做到SVG的支持
