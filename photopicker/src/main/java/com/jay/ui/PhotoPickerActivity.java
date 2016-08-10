package com.jay.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.jay.ui.entity.Photo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created on 2016/4/12
 *
 * @author Q.Jay
 * @version 1.0.0
 */
public class PhotoPickerActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String IS_MULTI_SELECT = "is_multi_select";//是否支持多选
    public static final String SELECT_RESULTS = "default_select";//默认选中某一个
    public static final String SELECT_RESULTS_ARRAY = "default_select_array";//默认选中某几个
    public static final String MAX_SELECT_SIZE = "max_select_size";//最大选择数量
    public static final String IS_SHOW_GIF = "is_show_gif";//是否显示gif图片
    //----------------------------------------------------------------------------------------------
    private static final String ALL_PHOTO_DIR_NAME = "全部相片";
    private static final int TAKE_PHOTO = 100;
    //----------------------------------------------------------------------------------------------
    private Toolbar toolbar;

    private RecyclerView rvContainer;
    private PhotoListAdapter photoListAdapter;
    private List<Photo> currentDisplayPhotos;//当前显示的图片
    //----------------------------------------------------------------------------------------------
    private boolean isShowGif;//是否显示gif图片
    private boolean isMultiSelect;//是否支持多选
    private int maxSize;//最大选择数量,当是多选的时候,单选直接忽略

    private ArrayList<String> menuDirs = new ArrayList<>();//目录集合,给菜单显示用的
    private Map<String, List<Photo>> photoDirMap = new ArrayMap<>();//图片目录名与目录下图片集合Map

    private ArrayList<String> resultPhotoUris;//选择的图片,如果是多选 - 需要保存此数据
    private String resultPhotoUri;//选择的图片,如果是单选 - 需要保存此数据
    private FloatingActionButton fab;
    private TextView toolbarCustomView;
    private File takePhotoFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            isMultiSelect = bundle.getBoolean(IS_MULTI_SELECT, false);
            isShowGif = bundle.getBoolean(IS_SHOW_GIF, false);
            if (isMultiSelect) {
                resultPhotoUris = bundle.getStringArrayList(SELECT_RESULTS_ARRAY);
                if (resultPhotoUris == null) {
                    resultPhotoUris = new ArrayList<>();
                }
                maxSize = bundle.getInt(MAX_SELECT_SIZE, 0);
            } else {
                resultPhotoUri = bundle.getString(SELECT_RESULTS);
            }
        }

        setContentView(R.layout.photo_picker_activity);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            if (isMultiSelect) {
                toolbarCustomView = new TextView(this);
                toolbarCustomView.setTextColor(Color.BLACK);
                toolbarCustomView.setTextSize(20);
                toolbarCustomView.setText(String.format(Locale.getDefault(), "%d/%d", resultPhotoUris.size(), maxSize));
                ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER_HORIZONTAL;
                actionBar.setCustomView(toolbarCustomView, params);
                actionBar.setDisplayShowCustomEnabled(true);
            }
        }
        toolbar.setTitle("图库");

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        rvContainer = (RecyclerView) findViewById(R.id.rvContainer);
        if (rvContainer != null) {
            rvContainer.setHasFixedSize(true);
            rvContainer.setLayoutManager(new GridLayoutManager(this, 3));
            rvContainer.setItemAnimator(new DefaultItemAnimator());
            photoListAdapter = new PhotoListAdapter(this, null);
            rvContainer.setAdapter(photoListAdapter);
        }

        fab = (FloatingActionButton) findViewById(R.id.tab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitAndOk();
            }
        });
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        @SuppressWarnings("UnnecessaryLocalVariable")
        CursorLoader cursorLoader = new CursorLoader(
                this,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME},
                "mime_type=? or mime_type=?" + (isShowGif ? "or mime_type=?" : ""),
                isShowGif ? new String[]{"image/jpeg", "image/png", "image/gif"} : new String[]{"image/jpeg", "image/png"},
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        );
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            List<Photo> allPhotoUris = new ArrayList<>();
            if (!photoDirMap.containsKey(ALL_PHOTO_DIR_NAME)) {
                photoDirMap.put(ALL_PHOTO_DIR_NAME, allPhotoUris);
            }
            if (!menuDirs.contains(ALL_PHOTO_DIR_NAME)) {
                menuDirs.add(ALL_PHOTO_DIR_NAME);
            }
            List<Photo> photos;
            do {
                String photoDir = data.getString(data.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME));
                String photoUri = data.getString(data.getColumnIndex(MediaStore.Images.Media.DATA));
                if (!photoDirMap.containsKey(photoDir)) {
                    photos = new ArrayList<>();
                    photoDirMap.put(photoDir, photos);
                    menuDirs.add(photoDir);
                } else {
                    photos = photoDirMap.get(photoDir);
                }
                Photo photo = new Photo();
//                photo.thumbnailsUri = thumbnailsUri;
                if (resultPhotoUris!= null && resultPhotoUris.contains(photoUri)) {
                    photo.isChecked = true;
                }
                photo.uri = photoUri;
                photos.add(photo);


                allPhotoUris.add(photo);
            } while (data.moveToNext());
            notifyChangePhotoList(ALL_PHOTO_DIR_NAME, allPhotoUris);

        }
    }

    private void notifyChangePhotoList(String subTitle, List<Photo> photos) {
        currentDisplayPhotos = photos;
        toolbar.setSubtitle(subTitle);
        photoListAdapter.setData(photos);
        photoListAdapter.notifyDataSetChanged();

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //do nothing
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuCamera = menu.add(Menu.NONE, R.id.menu_camera, 0, "拍照").setIcon(R.drawable.ic_photo_camera_black_24dp);
        MenuItemCompat.setShowAsAction(menuCamera, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        SubMenu menuDir = menu.addSubMenu(Menu.NONE, R.id.menu_dir, 1, "目录").setIcon(R.drawable.ic_folder_open_black_24dp);
        MenuItemCompat.setShowAsAction(menuDir.getItem(), MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_camera) {
            //用户要拍照
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePhotoFile = createImageFile();
            // Continue only if the File was successfully created
            if (takePhotoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(takePhotoFile));
                startActivityForResult(takePictureIntent, TAKE_PHOTO);
            } else {
                showSnackBar(getString(R.string.open_camera_fail));
            }
        } else if (item.getItemId() == R.id.menu_dir) {
            SubMenu subMenu = item.getSubMenu();
            subMenu.clear();
            for (String text : menuDirs) {
                subMenu.add(Menu.NONE, Menu.FIRST + 1, 0, text);
            }
        } else {
            String menuTitle = item.getTitle().toString();
            List<Photo> photos = photoDirMap.get(menuTitle);
            if (photos != null) {
                notifyChangePhotoList(menuTitle, photos);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp;
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        if (!storageDir.exists()) {
            if (!storageDir.mkdir()) {
                return null;
            }
        }
        File file = null;
        try {
            file = File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public void onPhotoListItemClick(View view) {
        fab.hide();
        int index = rvContainer.getChildViewHolder(view).getLayoutPosition();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        PhotoPreviewFragment f = PhotoPreviewFragment.newInstance(currentDisplayPhotos, index);
        ft.add(R.id.container, f, null);
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        fab.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PHOTO && resultCode == RESULT_OK) {
            if (takePhotoFile != null) {
                notifyMediaUpdate(takePhotoFile);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_MULTI_SELECT, isMultiSelect);
        outState.putBoolean(IS_SHOW_GIF, isShowGif);
        outState.putInt(MAX_SELECT_SIZE, maxSize);
        outState.putString("takePhotoPath", takePhotoFile.getAbsolutePath());
        if (isMultiSelect) {
            outState.putStringArrayList(SELECT_RESULTS_ARRAY, resultPhotoUris);
        } else {
            outState.putString(SELECT_RESULTS, resultPhotoUri);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isMultiSelect = savedInstanceState.getBoolean(IS_MULTI_SELECT, false);
        isShowGif = savedInstanceState.getBoolean(IS_SHOW_GIF, false);
        maxSize = savedInstanceState.getInt(MAX_SELECT_SIZE, 0);
        String takePhotoPath = savedInstanceState.getString("takePhotoPath");
        if (!TextUtils.isEmpty(takePhotoPath)) {
            takePhotoFile = new File(takePhotoPath);
        } else {
            showSnackBar(getString(R.string.photo_save_fail));
        }
        if (isMultiSelect) {
            resultPhotoUris = savedInstanceState.getStringArrayList(SELECT_RESULTS_ARRAY);
        } else {
            resultPhotoUri = savedInstanceState.getString(SELECT_RESULTS);
        }
    }

    public void notifyMediaUpdate(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    private void exitAndOk() {
        Intent data = new Intent();
        if (isMultiSelect) {
            if (resultPhotoUris != null && !resultPhotoUris.isEmpty()){
                data.putExtra(SELECT_RESULTS_ARRAY, resultPhotoUris);
            }else{
                showSnackBar("没有选择任何图片,请至少选择一张图片");
                return;
            }
        } else {
            if (!TextUtils.isEmpty(resultPhotoUri)){
                data.putExtra(SELECT_RESULTS, resultPhotoUri);
            }else{
                showSnackBar("没有选择任何图片,请选择一张图片");
                return;
            }
        }
        setResult(RESULT_OK, data);
        finish();
    }

//--------------------------------------------------------------------------------------------------

    /**
     * Adapter
     */
    class PhotoListAdapter extends RecyclerView.Adapter<PhotoListAdapter.PhotoVH> {
        private final List<Photo> photos = new ArrayList<>();
        private final Activity activity;

        public PhotoListAdapter(Activity activity, List<Photo> photos) {
            if (photos != null) {
                this.photos.addAll(photos);
            }
            this.activity = activity;
        }

        @Override
        public int getItemCount() {
            return photos.size();
        }

        public void setData(List<Photo> photoUris) {
            photos.clear();
            photos.addAll(photoUris);
        }

        @Override
        public PhotoVH onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            View itemView = LayoutInflater.from(context).inflate(R.layout.photo_img_item, parent, false);
            return new PhotoVH(itemView);
        }

        @Override
        public void onBindViewHolder(PhotoVH holder, int position) {
            ImageView photoView = holder.ivPhoto;
            Photo photo = photos.get(position);
            Glide.with(activity).load(photo.uri).thumbnail(.1f).centerCrop().crossFade().diskCacheStrategy(DiskCacheStrategy.RESULT).into(photoView);
            CheckBox checkBox = holder.checkbox;

            checkBox.setChecked(photo.isChecked);

            checkBox.setTag(position);
            checkBox.setOnClickListener(checkedChangeListener);
        }

        private View.OnClickListener checkedChangeListener = new View.OnClickListener() {
            private Photo lastPhoto;
            private int lastPosition;

            @Override
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                boolean checked = checkBox.isChecked();
                if (isMultiSelect && maxSize == resultPhotoUris.size() && checked) {
                    onAchieveMaxCount();
                    checkBox.setChecked(false);
                    return;
                }
                int position = (int) v.getTag();
                Photo photo = photos.get(position);
                photo.isChecked = checked;
                if (!isMultiSelect) {
                    //单选
                    if (lastPhoto != null) {
                        if (lastPhoto.equals(photo)) {
                            //如果操作的是同一张图片
                            lastPhoto = null;
                            resultPhotoUri = null;
                        }else{
                            /** 选择的是另一个图片 把上一个取消选中 */
                            lastPhoto.isChecked = false;
                            notifyItemChanged(lastPosition);
                            lastPhoto = photo;
                            lastPosition = position;
                        }
                    }else{
                        photo.isChecked = checked;
                        lastPhoto = photo;
                        lastPosition = position;
                    }
                    if (checked) {
                        resultPhotoUri = photo.uri;
                    }
                } else {
                    //多选
                    String uri = photo.uri;
                    if (checked) {
                        //添加选中的图片Uri,条件:选中 + 结果集合中没有此Uri
                        if (!resultPhotoUris.contains(uri)) {
                            resultPhotoUris.add(uri);
                        }
                    } else {
                        //移除,条件:未选中 + 结果集合中有此Uri
                        if (resultPhotoUris.contains(uri)) {
                            resultPhotoUris.remove(uri);
                        }
                    }
                    toolbarCustomView.setText(String.format(Locale.getDefault(), "%d/%d", resultPhotoUris.size(), maxSize));
                }
            }
        };

        class PhotoVH extends RecyclerView.ViewHolder {
            ImageView ivPhoto;
            CheckBox checkbox;

            public PhotoVH(View itemView) {
                super(itemView);
                ivPhoto = (ImageView) itemView.findViewById(R.id.ivPhoto);
                checkbox = (CheckBox) itemView.findViewById(R.id.checkbox);
            }

        }
    }

    private void onAchieveMaxCount() {
        showSnackBar(getString(R.string.max_options));
    }

    private void showSnackBar(CharSequence text) {
        Snackbar.make(fab, text, Snackbar.LENGTH_SHORT).show();
    }
}
