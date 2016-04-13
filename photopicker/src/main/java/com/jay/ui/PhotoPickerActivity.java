package com.jay.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
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
import com.jay.ui.entity.Photo;

import java.util.ArrayList;
import java.util.List;
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

    private ArrayList<String> resultPhotoUris;//选择的图片,如果是多选
    private String resultPhotoUri;//选择的图片,如果是单选
    private FloatingActionButton fab;
    private TextView toolbarCustomView;

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
            }else{
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
                toolbarCustomView.setText(String.format("%d/%d",resultPhotoUris.size(),maxSize));
                ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER_HORIZONTAL;
                actionBar.setCustomView(toolbarCustomView,params);
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
                new String[]{MediaStore.Images.Media.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME},
                "mime_type=? or mime_type=?" + (isShowGif ? "or mime_type=?" : ""),
                isShowGif ? new String[]{"image/jpeg", "image/png", "image/gif"} : new String[]{"image/jpeg", "image/png"},
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        );
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        boolean b = data.moveToFirst();
        List<Photo> allPhotoUris = new ArrayList<>();
        photoDirMap.clear();
        photoDirMap.put(ALL_PHOTO_DIR_NAME, allPhotoUris);
        menuDirs.add(ALL_PHOTO_DIR_NAME);
        if (b) {
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
                photo.uri = photoUri;
                photos.add(photo);
                allPhotoUris.add(photo);
            } while (data.moveToNext());
        }
        notifyChangePhotoList(ALL_PHOTO_DIR_NAME, allPhotoUris);

        Menu menu = toolbar.getMenu();
        SubMenu menuDir;
        try {
            menuDir = menu.getItem(1).getSubMenu();
            menuDir.clear();
        } catch (IndexOutOfBoundsException e) {
            menuDir = menu.addSubMenu(Menu.NONE, R.id.menu_dir, 0, "目录").setIcon(R.drawable.ic_folder_open_black_24dp);
            MenuItemCompat.setShowAsAction(menuDir.getItem(), MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        }
        for (String text : menuDirs) {
            menuDir.add(Menu.NONE, Menu.FIRST + 1, 0, text);
        }
    }

    private void notifyChangePhotoList(String subTitle, List<Photo> photos) {
        currentDisplayPhotos = photos;
        toolbar.setSubtitle(subTitle);
        if (photoListAdapter == null) {
            photoListAdapter = new PhotoListAdapter(this,photos);
            rvContainer.setAdapter(photoListAdapter);
        }else{
            photoListAdapter.setData(photos);
            photoListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //do nothing
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuCamera = menu.add(Menu.NONE, R.id.menu_camera, 0, "拍照").setIcon(R.drawable.ic_photo_camera_black_24dp);
        MenuItemCompat.setShowAsAction(menuCamera, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_camera) {
            //用户要拍照
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(takePictureIntent,100);
        } else {
            String menuTitle = item.getTitle().toString();
            List<Photo> photos = photoDirMap.get(menuTitle);
            if (photos != null) {
                notifyChangePhotoList(menuTitle, photos);
            }
        }
        return super.onOptionsItemSelected(item);
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
    }

    private void exitAndOk() {
        Intent data = new Intent();
        if (isMultiSelect) {
            data.putExtra(SELECT_RESULTS_ARRAY, resultPhotoUris);
        }else{
            data.putExtra(SELECT_RESULTS, resultPhotoUri);
        }
        setResult(RESULT_OK,data);
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
            this.photos.addAll(photos);
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
            ImageView photoView = holder.getView(R.id.ivPhoto);
            Photo photo = photos.get(position);
            Glide.with(activity).load(photo.uri).centerCrop().crossFade().into(photoView);
            CheckBox checkBox = holder.getView(R.id.checkbox);
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
                if (isMultiSelect && maxSize == resultPhotoUris.size()) {
                    onAchieveMaxCount();
                    checkBox.setChecked(false);
                    return;
                }

                int position = (int) v.getTag();
                Photo photo = currentDisplayPhotos.get(position);
                boolean checked = checkBox.isChecked();
                photo.isChecked = checked;
                String uri = photo.uri;
                if (!isMultiSelect) {
                    //单选
                    if (lastPhoto != null && !lastPhoto.equals(photo)) {
                        lastPhoto.isChecked = false;
                        photoListAdapter.notifyItemChanged(lastPosition);
                    }
                    lastPhoto = photo;
                    lastPosition = position;
                    if (checked) {
                        resultPhotoUri = uri;
                    }
                }else{
                    //多选
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
                    toolbarCustomView.setText(String.format("%d/%d",resultPhotoUris.size(),maxSize));
                }
            }
        };

        class PhotoVH extends RecyclerView.ViewHolder {
            private SparseArray<View> views = new SparseArray<>();
            public PhotoVH(View itemView) {
                super(itemView);
            }

            @SuppressWarnings("unchecked")
            public <V extends View> V getView(@IdRes int viewId) {
                View view = views.get(viewId);
                if (view == null) {
                    view = itemView.findViewById(viewId);
                }
                return (V) view;
            }
        }
    }

    private void onAchieveMaxCount() {
        Snackbar.make(fab,"达到最大选择数量",Snackbar.LENGTH_SHORT).show();
    }
}
