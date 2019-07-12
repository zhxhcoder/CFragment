package com.creditease.cfragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public abstract class AbstractBaseFragment extends Fragment {

    public abstract int initContentView(); // 初始化Fragment的整个View

    public abstract void onFirstVisible(); // Fragment可见的时候

    private View mRootView; // onCreateView()里返回的view，修饰为protected,所以子类继承该类时，在onCreateView里必须对该变量进行初始化

    private boolean mIsVisible = false; // Fragment当前状态是否可见

    private boolean mIsViewCreated = false; // Fragment的view是否已创建

    private boolean mIsFirstVisible = true; // 第一次可见

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRootView == null) {
            mRootView = inflater.inflate(initContentView(), container, false);
        }
        ViewGroup parent = (ViewGroup) mRootView.getParent();
        if (parent != null) {
            parent.removeView(mRootView);
        }
        mIsViewCreated = true;
        return mRootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 获取参数
        if (getArguments() != null) {
            initParams(getArguments());
        }
        // 初始化控件
        initView(view);
    }

    @Override
    public void onResume() { // 和activity的onResume绑定，Fragment初始化的时候必调用，但切换fragment的hide和visible的时候可能不会调用
        super.onResume();
        if (isAdded() && !isHidden()) { // 用isVisible此时为false，因为mView.getWindowToken为null
            onFragmentVisible();
            mIsVisible = true;
        }
    }

    @Override
    public void onPause() {
        if (isVisible() || mIsVisible) {
            onFragmentInVisible();
            mIsVisible = false;
        }
        super.onPause();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        // view没有创建的时候不进行操作
        if (!mIsViewCreated) {
            return;
        }
        // 确保在一个可见周期中只调用一次onFragmentVisible()
        if (getUserVisibleHint()) {
            if (!mIsVisible) {
                mIsVisible = true;
                onFragmentVisible();
            }
        } else {
            if (mIsVisible) {
                mIsVisible = false;
                onFragmentInVisible();
            }
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) { //
        // 默认fragment创建的时候是可见的，但是不会调用该方法！切换可见状态的时候会调用，但是调用onResume，onPause的时候却不会调用
        super.onHiddenChanged(hidden);
        if (!hidden) {
            onFragmentVisible();
            mIsVisible = true;
        } else {
            onFragmentInVisible();
            mIsVisible = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mIsViewCreated = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * 方法描述：初始化参数
     *
     * @author renmeng
     * @time 2018/5/30
     */
    public void initParams(Bundle bundle) {

    }

    /**
     * 方法描述：初始化View
     *
     * @author renmeng
     * @time 2018/11/27
     */
    public void initView(View view) {

    }

    /**
     * 方法描述：fragment可见
     *
     * @author renmeng
     * @time 2018/5/30
     */
    public void onFragmentVisible() {
        if (mIsFirstVisible) {
            mIsFirstVisible = false;

            LogUtils.i(getClass().getSimpleName() + "  onFirstVisible");
            onFirstVisible();
        } else {
            onEveryVisible();
        }
    }

    /**
     * 方法描述：除了第一次可见外，每次可见都调用
     *
     * @author renmeng
     * @time 2018/5/30
     */
    public void onEveryVisible() {
        LogUtils.i(getClass().getSimpleName() + "  onEveryVisible");
    }

    /**
     * 方法描述：Fragment不可见的时候操作,onPause的时候,以及不可见的时候调用
     *
     * @author renmeng
     * @time 2018/5/30
     */
    public void onFragmentInVisible() {
        LogUtils.i(getClass().getSimpleName() + "  onFragmentInVisible");
    }

    /**
     * 方法描述：findViewById封装
     *
     * @author renmeng
     * @time 2018/5/30
     */
    public <T extends View> T findView(View view, int id) {
        return (T) view.findViewById(id);
    }
}
