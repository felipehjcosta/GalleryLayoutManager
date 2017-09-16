package com.github.felipehjcosta.gallerylayoutmanager;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.github.felipehjcosta.gallerylayoutmanager.adapter.DemoAdapter;
import com.github.felipehjcosta.gallerylayoutmanager.base.BaseRestoreFragment;
import com.github.felipehjcosta.gallerylayoutmanager.layout.impl.ScaleTransformer;
import com.github.felipehjcosta.layoutmanager.GalleryLayoutManager;

public class TestFragment extends BaseRestoreFragment {

    @BindView(com.github.felipehjcosta.gallerylayoutmanager.R.id.main_recycle1)
    RecyclerView mMainRecycle1;
    @BindView(com.github.felipehjcosta.gallerylayoutmanager.R.id.main_tv_recycle_info_1)
    TextView mMainTvRecycleInfo1;
    @BindView(com.github.felipehjcosta.gallerylayoutmanager.R.id.main_recycle2)
    RecyclerView mMainRecycle2;
    @BindView(com.github.felipehjcosta.gallerylayoutmanager.R.id.main_tv_recycle_info_2)
    TextView mMainTvRecycleInfo2;
    @BindView(com.github.felipehjcosta.gallerylayoutmanager.R.id.main_tv_recycle_info_3)
    TextView mMainTvRecycleInfo3;
    @BindView(com.github.felipehjcosta.gallerylayoutmanager.R.id.main_btn_random)
    Button mMainBtnRandom;

    public static TestFragment newInstance() {

        Bundle args = new Bundle();

        TestFragment fragment = new TestFragment();
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * Generate by live templates.
     * Use FragmentManager to find this Fragment's instance by tag
     */
    public static TestFragment findFragment(FragmentManager manager) {
        return (TestFragment) manager.findFragmentByTag(TestFragment.class.getSimpleName());
    }

    @Override
    protected View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(com.github.felipehjcosta.gallerylayoutmanager.R.layout.fragment_test, container, false);
    }

    @Override
    protected void initView(View root, Bundle savedInstanceState) {
        ButterKnife.bind(this, root);
        final List<String> title = new ArrayList<String>();
        int size = 50;
        for (int i = 0; i < size; i++) {
            title.add("Hello" + i);
        }
        GalleryLayoutManager layoutManager1 = new GalleryLayoutManager(GalleryLayoutManager.HORIZONTAL);
        layoutManager1.attach(mMainRecycle1, 0);
        layoutManager1.setItemTransformer(new ScaleTransformer());
        DemoAdapter demoAdapter1 = new DemoAdapter(title) {
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                mMainTvRecycleInfo1.append("Create VH type:+" + viewType + "\n");
                return super.onCreateViewHolder(parent, viewType);
            }
        };
        demoAdapter1.setOnItemClickListener(new DemoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                mMainRecycle1.smoothScrollToPosition(position);
            }
        });
        mMainRecycle1.setAdapter(demoAdapter1);

        final GalleryLayoutManager layoutManager2 = new GalleryLayoutManager(GalleryLayoutManager.VERTICAL);
        layoutManager2.attach(mMainRecycle2, 20);
        layoutManager2.setCallbackInFling(true);
        layoutManager2.setOnItemSelectedListener(new GalleryLayoutManager.OnItemSelectedListener() {
            @Override
            public void onItemSelected(RecyclerView recyclerView, View item, int position) {
                mMainTvRecycleInfo2.setText("selected:" + position + "\n");
            }
        });
        DemoAdapter demoAdapter2 = new DemoAdapter(title, DemoAdapter.VIEW_TYPE_TEXT);
        demoAdapter2.setOnItemClickListener(new DemoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                mMainRecycle2.smoothScrollToPosition(position);
            }
        });
        mMainRecycle2.setAdapter(demoAdapter2);
    }


    @Override
    protected void initData(Bundle savedInstanceState) {

    }

    private final Random mRandom = new Random();

    @OnClick({com.github.felipehjcosta.gallerylayoutmanager.R.id.main_btn_random, com.github.felipehjcosta.gallerylayoutmanager.R.id.main_btn_data_change})
    public void onClick(View view) {
        if (view.getId() == com.github.felipehjcosta.gallerylayoutmanager.R.id.main_btn_random) {
            int selectPosition = mRandom.nextInt(50);
            mMainRecycle1.smoothScrollToPosition(selectPosition);
            mMainRecycle2.smoothScrollToPosition(selectPosition);
        } else {
            if (mMainRecycle1.getAdapter() instanceof DemoAdapter) {
                int result = ((DemoAdapter) mMainRecycle1.getAdapter()).dataChange();
                if (result == 1) {
                    Toast.makeText(getContext(), "add data", Toast.LENGTH_SHORT).show();
                } else if (result == -1) {
                    Toast.makeText(getContext(), "remove data", Toast.LENGTH_SHORT).show();
                }
            }
            if (mMainRecycle2.getAdapter() instanceof DemoAdapter) {
                ((DemoAdapter) mMainRecycle2.getAdapter()).dataChange();
            }
        }
    }

}
