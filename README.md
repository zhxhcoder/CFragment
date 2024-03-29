# CFragment
简单生命周期的fragment


```java
/**
 * Created by zhxh on 2019/06/20
 * 支持嵌套
 * onFirstVisible
 * onEveryVisible
 * onFragmentInVisible
 */
public abstract class CFragment extends Fragment {

    private boolean mIsFirstVisible = true;

    private boolean isViewCreated = false;

    private boolean currentVisibleState = false;

    protected View mRootView; // onCreateView()里返回的view，修饰为protected,所以子类继承该类时，在onCreateView里必须对该变量进行初始化

    public abstract int initContentView(); // 初始化Fragment的整个View

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        // 对于默认 tab 和 间隔 checked tab 需要等到 isViewCreated = true 后才可以通过此通知用户可见
        // 这种情况下第一次可见不是在这里通知 因为 isViewCreated = false 成立,等从别的界面回到这里后会使用 onEveryVisible 通知可见
        // 对于非默认 tab mIsFirstVisible = true 会一直保持到选择则这个 tab 的时候，因为在 onActivityCreated 会返回 false
        if (isViewCreated) {
            if (isVisibleToUser && !currentVisibleState) {
                dispatchUserVisibleHint(true);
            } else if (!isVisibleToUser && currentVisibleState) {
                dispatchUserVisibleHint(false);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 获取参数
        if (getArguments() != null) {
            initParams(getArguments());
        }

        if (mRootView == null) {
            mRootView = inflater.inflate(initContentView(), container, false);
        }
        ViewGroup parent = (ViewGroup) mRootView.getParent();
        if (parent != null) {
            parent.removeView(mRootView);
        }
        isViewCreated = true;
        return mRootView;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 初始化控件
        initView(view);
    }
    public void initView(View root) {

    }
    public void initParams(Bundle arguments) {
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        isViewCreated = true;
        // !isHidden() 默认为 true  在调用 hide show 的时候可以使用
        if (!isHidden() && getUserVisibleHint()) {
            // 这里的限制只能限制 A - > B 两层嵌套
            dispatchUserVisibleHint(true);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            dispatchUserVisibleHint(false);
        } else {
            dispatchUserVisibleHint(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mIsFirstVisible) {
            if (!isHidden() && !currentVisibleState && getUserVisibleHint()) {
                dispatchUserVisibleHint(true);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 当前 Fragment 包含子 Fragment 的时候 dispatchUserVisibleHint 内部本身就会通知子 Fragment 不可见
        // 子 fragment 走到这里的时候自身又会调用一遍 ？
        if (currentVisibleState && getUserVisibleHint()) {
            dispatchUserVisibleHint(false);
        }
    }

    /**
     * 统一处理 显示隐藏
     *
     * @param visible
     */
    private void dispatchUserVisibleHint(boolean visible) {
        //当前 Fragment 是 child 时候 作为缓存 Fragment 的子 fragment getUserVisibleHint = true
        //但当父 fragment 不可见所以 currentVisibleState = false 直接 return 掉
        // 这里限制则可以限制多层嵌套的时候子 Fragment 的分发
        if (visible && isParentInvisible()) return;
        //此处是对子 Fragment 不可见的限制，因为 子 Fragment 先于父 Fragment回调本方法 currentVisibleState 置位 false
        // 当父 dispatchChildVisibleState 的时候第二次回调本方法 visible = false 所以此处 visible 将直接返回
        if (currentVisibleState == visible) {
            return;
        }

        currentVisibleState = visible;

        if (visible) {
            if (!isAdded()) return;
            if (mIsFirstVisible) {
                onFirstVisible();
                onEveryVisible(true);
                mIsFirstVisible = false;
            } else {
                onEveryVisible(false);
            }
            onEveryVisible();
            enqueueDispatchVisible();
        } else {
            dispatchChildVisibleState(false);
            onFragmentInVisible();
        }
    }

    /**
     * 由于下 onFirstVisible 中添加ViewPager Adapter 的时候由于异步提交导致 先派发了 子fragment 的 onFirstVisible
     * 造成空指针 所以将可见事件派发主线成
     */
    private void enqueueDispatchVisible() {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                dispatchChildVisibleState(true);
            }
        });
    }

    public boolean ismIsFirstVisible() {
        return mIsFirstVisible;
    }

    private Handler mHandler;

    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }


    /**
     * 用于分发可见时间的时候父获取 fragment 是否隐藏
     *
     * @return true fragment 不可见， false 父 fragment 可见
     */
    private boolean isParentInvisible() {
        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof CFragment) {
            CFragment fragment = (CFragment) parentFragment;
            return !fragment.isSupportVisible();
        } else {
            return false;
        }
    }

    private boolean isSupportVisible() {
        return currentVisibleState;
    }

    /**
     * 当前 Fragment 是 child 时候 作为缓存 Fragment 的子 fragment 的唯一或者嵌套 VP 的第一 fragment 时 getUserVisibleHint = true
     * 但是由于父 Fragment 还进入可见状态所以自身也是不可见的， 这个方法可以存在是因为庆幸的是 父 fragment 的生命周期回调总是先于子 Fragment
     * 所以在父 fragment 设置完成当前不可见状态后，需要通知子 Fragment 我不可见，你也不可见，
     * <p>
     * 因为 dispatchUserVisibleHint 中判断了 isParentInvisible 所以当 子 fragment 走到了 onActivityCreated 的时候直接 return 掉了
     * <p>
     * 当真正的外部 Fragment 可见的时候，走 setVisibleHint (VP 中)或者 onActivityCreated (hide show) 的时候
     * 从对应的生命周期入口调用 dispatchChildVisibleState 通知子 Fragment 可见状态
     * <p>
     * //bug fix Fragment has not been attached yet
     *
     * @param visible
     */
    private void dispatchChildVisibleState(boolean visible) {
        if (!isAdded()) return;
        FragmentManager childFragmentManager = getChildFragmentManager();
        List<Fragment> fragments = childFragmentManager.getFragments();
        if (!fragments.isEmpty()) {
            for (Fragment child : fragments) {
                if (child instanceof CFragment && child.isAdded() && !child.isHidden() && child.getUserVisibleHint()) {
                    ((CFragment) child).dispatchUserVisibleHint(visible);
                }
            }
        }
    }

    /**
     * 是否已经初始化 View 完毕
     * 这里是指如果在 onFirstVisible 中去初始化 View 的时候
     * 而 initView(View view) 初始化View 时机是在 inflate 布局后
     *
     * @return 是否已经初始化 View 完毕
     */
    public boolean hasFirstVisible() {
        return !mIsFirstVisible;
    }

    public void onFirstVisible() {
        LogUtil.i(getClass().getSimpleName() + "  对用户第一次可见");
    }

    /**
     * 每次可见都回调
     */
    public void onEveryVisible() {
        LogUtil.i(getClass().getSimpleName() + "  对用户可见");
    }

    /**
     * 添加是否是第一次可见的标识 切勿和 onEveryVisible 同时使用因为两个方法回调时机一样
     *
     * @param firstVisible true 是第一次可见 == onFirstVisible  false 去除第一次回调
     */
    public void onEveryVisible(boolean firstVisible) {
        LogUtil.i(getClass().getSimpleName() + "  对用户可见  firstVisible = " + firstVisible);
    }

    public void onFragmentInVisible() {
        LogUtil.i(getClass().getSimpleName() + "  对用户不可见");
    }


    // 初始化Fragment的整个View

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false;
        mIsFirstVisible = true;
    }
}
```

