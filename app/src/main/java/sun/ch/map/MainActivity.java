package sun.ch.map;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.TextOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.DrivingRouteOverlay;
import com.baidu.mapapi.overlayutil.PoiOverlay;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.poi.PoiSortType;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {
    MapView mMapView = null;
    private BaiduMap mBaiduMap;
    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new MyLocationListener();

    private ArrayList<String> list = new ArrayList<String>();
    private Marker marker;

    private BDLocation mLocation;
    private TextView text;
    private Overlay textOverlay;
    private PoiSearch mPoiSearch;

    private ArrayList<String> addressList = new ArrayList<String>();
    private ListView listview;
    private MyAdapter myAdapter;
    private EditText et_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        //声明LocationClient类
        mLocationClient = new LocationClient(getApplicationContext());
        //注册监听函数
        initLocation();//该类用来设置定位SDK的定位方式
        mLocationClient.registerLocationListener(myListener);

        mLocationClient.start();//开启定位


        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();

        Spinner mSpinner = (Spinner) findViewById(R.id.spinner);
        list.add("开启标注");
        list.add("清除标注");
        list.add("普通地图");
        list.add("卫星地图");
        list.add("文字覆盖");
        list.add("取消文字覆盖");
        list.add("显示我的位置");
        list.add("取消显示我的位置");
        list.add("移动到我的位置");
        mSpinner.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, list));
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        mark();
                        break;
                    case 1:
                        //mBaiduMap.clear();
                        if (marker != null) {
                            marker.remove();
                        }
                        break;
                    case 2:
                        //普通地图
                        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                        break;
                    case 3:
                        //卫星地图
                        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                        break;
                    case 4:
                        text();//文字覆盖
                        break;
                    case 5:
                        if (textOverlay != null) {
                            textOverlay.remove();
                        }
                        break;
                    case 6:
                        setLocation();
                        break;
                    case 7:
                        // 当不需要定位图层时关闭定位图层
                        mBaiduMap.setMyLocationEnabled(false);
                        break;
                    case 8:

                        moveToMyLocation();

                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        poisearch();//调用检索功能

    }

    //检索功能
    private void poisearch() {

        listview = (ListView) findViewById(R.id.listview);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String address = addressList.get(i);
                et_text.setText(address);
                et_text.setSelection(address.length());//光标设置到最末尾
                listview.setVisibility(View.GONE);
            }
        });

        //创建POI检索实例
        mPoiSearch = PoiSearch.newInstance();
        //创建POI检索监听者

        OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener() {
            public void onGetPoiResult(PoiResult result) {
                //添加查询结果地点标注图层
                mBaiduMap.clear();
                PoiOverlay poiOverlay = new PoiOverlay(mBaiduMap);
                poiOverlay.setData(result);
                poiOverlay.addToMap();

                addressList.clear();

                //获取POI检索结果
                List<PoiInfo> allPoi = result.getAllPoi();
                if (allPoi != null) {
                    for (PoiInfo poi : allPoi) {
                        String address = poi.address;
                        String phoneNum = poi.phoneNum;
                        String name = poi.name;
                       /* String s = new String();
                        s = "address" + address + "phoneNum" + phoneNum + "name" + name;
                        Log.d("MainActivity",s);*/
                        addressList.add(address);
                    }
                    if(addressList!=null){
                        if(myAdapter == null){
                            listview.setVisibility(View.VISIBLE);
                            myAdapter = new MyAdapter();
                            listview.setAdapter(myAdapter);
                        }else {
                            myAdapter.notifyDataSetChanged();
                        }

                    }

                } else {
                    Toast.makeText(MainActivity.this, "没有数据", Toast.LENGTH_SHORT).show();
                }

            }

            public void onGetPoiDetailResult(PoiDetailResult result) {
                //获取Place详情页检索结果
            }

            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

            }
        };
        //设置POI检索监听者
        mPoiSearch.setOnGetPoiSearchResultListener(poiListener);

        et_text = (EditText) findViewById(R.id.et_text);
        ImageButton searchBtn = (ImageButton) findViewById(R.id.searchBtn);
        //当输入框内容改变时发起检索
        et_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String details = et_text.getText().toString().trim();
                if (!TextUtils.isEmpty(details)) {
                    //发起检索请求
                    if (mLocation != null) {
                        /*PoiCitySearchOption poiCitySearchOption = new PoiCitySearchOption();//城市内搜索
                        poiCitySearchOption.city(mLocation.getCity());
                        poiCitySearchOption.keyword(details);
                        poiCitySearchOption.pageNum(0);
                        mPoiSearch.searchInCity(poiCitySearchOption);*/
                        PoiNearbySearchOption poiNearbySearchOption = new PoiNearbySearchOption();//坐标附近点搜索
                        LatLng latLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                        poiNearbySearchOption.location(latLng);
                        poiNearbySearchOption.keyword(details);
                        poiNearbySearchOption.pageCapacity(100).pageNum(0).radius(10 * 1000).sortType(PoiSortType.distance_from_near_to_far);
                        mPoiSearch.searchNearby(poiNearbySearchOption);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "输入框不能为空", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //当输入框内容发生改变时发起检索
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String enName = et_text.getText().toString().trim();
                driver(enName);//调用驾车线路规划
            }
        });

    }

    //驾车线路规划
    private void driver(String enName){
        //创建驾车线路规划检索实例
        RoutePlanSearch routePlanSearch = RoutePlanSearch.newInstance();
        //创建驾车线路规划检索监听者
        OnGetRoutePlanResultListener listener = new OnGetRoutePlanResultListener() {

            private DrivingRouteOverlay drivingRouteOverlay;

            public void onGetWalkingRouteResult(WalkingRouteResult result) {
                //获取步行线路规划结果
            }
            public void onGetTransitRouteResult(TransitRouteResult result) {
                //获取公交换乘路径规划结果
            }

            public void onGetDrivingRouteResult(DrivingRouteResult result) {
                if(result == null || SearchResult.ERRORNO.RESULT_NOT_FOUND == result.error){
                    Toast.makeText(getApplicationContext(), "未搜索到结果", Toast.LENGTH_LONG).show();
                    return;
                }
                //获取驾车线路规划结果
                if (result.error == SearchResult.ERRORNO.NO_ERROR) {
                    //在规划之前先移除原先的图层
                    if (drivingRouteOverlay!=null) {
                        drivingRouteOverlay.removeFromMap();
                        drivingRouteOverlay = null;
                    }
                    drivingRouteOverlay = new DrivingRouteOverlay(mBaiduMap);
                    DrivingRouteLine drivingRouteLine = result.getRouteLines().get(0);
                    text.setText(drivingRouteLine.getDistance()+"米"+drivingRouteLine.getDuration()/60+"分钟");
                    drivingRouteOverlay.setData(result.getRouteLines().get(0));
                    drivingRouteOverlay.addToMap();
                    drivingRouteOverlay.zoomToSpan();
                }

            }

            @Override
            public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

            }

            @Override
            public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

            }

            @Override
            public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

            }
        };
        //设置驾车线路规划检索监听者
        routePlanSearch.setOnGetRoutePlanResultListener(listener);
        //准备检索起、终点信息
        PlanNode stNode = PlanNode.withCityNameAndPlaceName(mLocation.getCity(),mLocation.getAddrStr());
        PlanNode enNode = PlanNode.withCityNameAndPlaceName(mLocation.getCity(), enName);
        //发起驾车线路规划检索
        routePlanSearch.drivingSearch((new DrivingRoutePlanOption())
                .from(stNode)
                .to(enNode));
    }

    //地图标注
    private void mark() {
        if (mLocation != null) {
            //定义Maker坐标点
            LatLng point = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
            //构建Marker图标
            BitmapDescriptor bitmap = BitmapDescriptorFactory
                    .fromResource(R.mipmap.icon_openmap_mark);
            //构建MarkerOption，用于在地图上添加Marker
            OverlayOptions option = new MarkerOptions()
                    .position(point)
                    .icon(bitmap);
            //在地图上添加Marker，并显示
            marker = (Marker) mBaiduMap.addOverlay(option);
        }

    }

    //文字覆盖
    private void text() {
        if (mLocation != null) {
            //定义文字所显示的坐标点
            LatLng llText = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
            //构建文字Option对象，用于在地图上添加文字
            OverlayOptions textOption = new TextOptions()
                    .bgColor(0xAAFFFF00)
                    .fontSize(24)
                    .fontColor(0xFFFF00FF)
                    .text(mLocation.getAddrStr())
                    .rotate(0)
                    .position(llText);
            //在地图上添加该文字对象并显示
            textOverlay = mBaiduMap.addOverlay(textOption);
        }

    }

    //显示我的位置
    private void setLocation() {
        if (mLocation != null) {

            // 构造定位数据
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(mLocation.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(mLocation.getLatitude())
                    .longitude(mLocation.getLongitude()).build();
            // 设置定位数据
            // 开启定位图层
            mBaiduMap.setMyLocationEnabled(true);
            mBaiduMap.setMyLocationData(locData);

        }

    }

    //移动到我的位置
    private void moveToMyLocation() {
        if (mLocation != null) {
            // 让地图中心跑到我的真实位置处
            LatLng latLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngZoom(latLng, 18);//注意设置缩放级别
            mBaiduMap.animateMapStatus(mapStatusUpdate, 1000);
        }
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备

        option.setCoorType("bd09ll");
        //可选，默认gcj02，设置返回的定位结果坐标系

        int span = 5000;
        option.setScanSpan(span);
        //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的

        option.setIsNeedAddress(true);
        //可选，设置是否需要地址信息，默认不需要

        option.setOpenGps(true);
        //可选，默认false,设置是否使用gps

        option.setLocationNotify(true);
        //可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果

        option.setIsNeedLocationDescribe(true);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”

        option.setIsNeedLocationPoiList(true);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到

        option.setIgnoreKillProcess(false);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死

        option.SetIgnoreCacheException(false);
        //可选，默认false，设置是否收集CRASH信息，默认收集

        option.setEnableSimulateGps(false);
        //可选，默认false，设置是否需要过滤GPS仿真结果，默认需要

        mLocationClient.setLocOption(option);
    }

    private boolean isFirst = true;//定义一个开关，进入时移动到我的位置一次即可

    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            mLocation = location;

            if (isFirst) {
                moveToMyLocation();//移动到我的位置
                isFirst = false;
            }

            text = (TextView) findViewById(R.id.textview);
            if (mLocation != null) {
                text.setText(mLocation.getAddrStr());
            }

            //获取定位结果
            StringBuffer sb = new StringBuffer(256);

            sb.append("time : ");
            sb.append(location.getTime());    //获取定位时间

            sb.append("\nerror code : ");
            sb.append(location.getLocType());    //获取类型类型

            sb.append("\nlatitude : ");
            sb.append(location.getLatitude());    //获取纬度信息

            sb.append("\nlontitude : ");
            sb.append(location.getLongitude());    //获取经度信息

            sb.append("\nradius : ");
            sb.append(location.getRadius());    //获取定位精准度

            if (location.getLocType() == BDLocation.TypeGpsLocation) {

                // GPS定位结果
                sb.append("\nspeed : ");
                sb.append(location.getSpeed());    // 单位：公里每小时

                sb.append("\nsatellite : ");
                sb.append(location.getSatelliteNumber());    //获取卫星数

                sb.append("\nheight : ");
                sb.append(location.getAltitude());    //获取海拔高度信息，单位米

                sb.append("\ndirection : ");
                sb.append(location.getDirection());    //获取方向信息，单位度

                sb.append("\naddr : ");
                sb.append(location.getAddrStr());    //获取地址信息

                sb.append("\ndescribe : ");
                sb.append("gps定位成功");

            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {

                // 网络定位结果
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());    //获取地址信息

                sb.append("\noperationers : ");
                sb.append(location.getOperators());    //获取运营商信息

                sb.append("\ndescribe : ");
                sb.append("网络定位成功");

            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {

                // 离线定位结果
                sb.append("\ndescribe : ");
                sb.append("离线定位成功，离线定位结果也是有效的");

            } else if (location.getLocType() == BDLocation.TypeServerError) {

                sb.append("\ndescribe : ");
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");

            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {

                sb.append("\ndescribe : ");
                sb.append("网络不同导致定位失败，请检查网络是否通畅");

            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {

                sb.append("\ndescribe : ");
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");

            }

            sb.append("\nlocationdescribe : ");
            sb.append(location.getLocationDescribe());    //位置语义化信息

            List<Poi> list = location.getPoiList();    // POI数据
            if (list != null) {
                sb.append("\npoilist size = : ");
                sb.append(list.size());
                for (Poi p : list) {
                    sb.append("\npoi= : ");
                    sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
                }
            }

            System.out.println(sb.toString());
        }


    }

    private class MyAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return addressList.size();
        }

        @Override
        public Object getItem(int i) {
            return addressList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, android.view.View view, ViewGroup viewGroup) {
            ViewHolder holder;
            if(view == null){
                holder = new ViewHolder();
                view = android.view.View.inflate(MainActivity.this,R.layout.list_item,null);
                holder.address = (TextView) view.findViewById(R.id.address);
                view.setTag(holder);
            }else {
                holder = (ViewHolder) view.getTag();
            }
            holder.address.setText(addressList.get(i));
            return view;
        }
    }

    private class ViewHolder{
        public TextView address;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        mLocationClient.stop();//关闭定位
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

}
