package com.penta.rxart;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    private final static String TAG = "rxPenta";
    @BindView(R.id.ll)
    LinearLayout ll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        rxGetData();
    }

    public void getData() {
        //异步获取图片
        new AsyncTask<Void, Void, List<Girl>>() {

            @Override
            protected List<Girl> doInBackground(Void... voids) {
                List<Girl> girlList = new ArrayList<>();
                try {
                    URL url = new URL("http://gank.io/api/data/%E7%A6%8F%E5%88%A9/10/2");
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.connect();
                    if (urlConnection.getResponseCode() == 200) {
                        // 获取返回的数据
                        String result = streamToString(urlConnection.getInputStream());
                        Log.e(TAG, "Get方式请求成功，result--->" + result);
                        JSONObject jsonObject = JSON.parseObject(result);
                        String results = jsonObject.getString("results");

                        if (null != results)
                            girlList = JSON.parseArray(results, Girl.class);

                    } else {
                        Log.e(TAG, "Get方式请求失败");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return girlList;
            }

            @Override
            protected void onPostExecute(final List<Girl> girlList) {
                new AsyncTask<Void, Void, List<Bitmap>>() {
                    @Override
                    protected List<Bitmap> doInBackground(Void... voids) {
                        List<Bitmap> bitmapList = new ArrayList<>();
                        for (Girl girl : girlList)
                            try {
                                //创建一个url对象
                                URL url = new URL(girl.getUrl());
                                //打开URL对应的资源输入流
                                InputStream is = url.openStream();
                                //从InputStream流中解析出图片
                                Bitmap bitmap = BitmapFactory.decodeStream(is);
                                //关闭输入流
                                is.close();
                                bitmapList.add(bitmap);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        return bitmapList;
                    }

                    @Override
                    protected void onPostExecute(List<Bitmap> bitmapList) {
                        //显示从网上下载的图片
                        for (Bitmap bitmap : bitmapList) {
                            ImageView imageView = new ImageView(MainActivity.this);
                            imageView.setImageBitmap(bitmap);
                            ll.addView(imageView);
                        }
                        super.onPostExecute(bitmapList);
                    }

                }.execute();
            }

        }.execute();
    }


    private void rxGetData () {

        Observable.create(new Observable.OnSubscribe<HttpURLConnection>() {
            @Override
            public void call(Subscriber<? super HttpURLConnection> subscriber) {
                        try {
                            URL url = new URL("http://gank.io/api/data/%E7%A6%8F%E5%88%A9/10/2");
                            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                            urlConnection.connect();
                            subscriber.onNext(urlConnection);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                })
                .filter(new Func1<HttpURLConnection, Boolean>() {
                    @Override
                    public Boolean call(HttpURLConnection urlConnection) {
                        try {

                            if (urlConnection.getResponseCode() == 200) {
                                return true;
                            } else {
                                Log.e(TAG, "Get方式请求失败");
                                return false;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return false;
                    }
                })

                .map(new Func1<HttpURLConnection, String>() {
                    @Override
                    public String call(HttpURLConnection httpURLConnection) {
                        JSONObject jsonObject = null;
                        try {
                            String result = streamToString(httpURLConnection.getInputStream());
                            Log.e(TAG, "Get方式请求成功，result--->" + result);
                            jsonObject = JSON.parseObject(result);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String results = jsonObject.getString("results");
                        return results;
                    }
                })
                .map(new Func1<String, List<Girl>>() {
                    @Override
                    public List<Girl> call(String results) {
                        List<Girl> girlList = null;
                        if (null != results)
                            girlList = JSON.parseArray(results, Girl.class);
                        return girlList;
                    }
                })
                .flatMap(new Func1<List<Girl>, Observable<Girl>>() {
                    @Override
                    public Observable<Girl> call(List<Girl> girlList) {
                        return Observable.from(girlList);
                    }
                })

                .map(new Func1<Girl, Bitmap>() {
                    @Override
                    public Bitmap call(Girl girl) {
                        Bitmap bitmap = null;
                        try {
                            //创建一个url对象
                            URL url = new URL(girl.getUrl());
                            //打开URL对应的资源输入流
                            InputStream is = url.openStream();
                            //从InputStream流中解析出图片
                            bitmap = BitmapFactory.decodeStream(is);
                            //关闭输入流
                            is.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return bitmap;
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Bitmap>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        ImageView imageView = new ImageView(MainActivity.this);
                        imageView.setImageBitmap(bitmap);
                        ll.addView(imageView);
                    }
                });
    }


    public String streamToString(InputStream is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[50000];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            baos.close();
            is.close();
            byte[] byteArray = baos.toByteArray();
            return new String(byteArray);
        } catch (Exception e) {
            return null;
        }
    }

    public void RxStudent() {
        List<Student> studentList = new ArrayList<>();

        Student student1 = new Student();
        student1.setAge(5);
        List<Course> courseList1 = new ArrayList<>();
        Course course1 = new Course();
        course1.setName("英语");
        Course course2 = new Course();
        course2.setName("数学");
        courseList1.add(course1);
        courseList1.add(course2);
        student1.setCourseList(courseList1);
        studentList.add(student1);

        Student student2 = new Student();
        student2.setAge(8);
        Course course3 = new Course();
        course3.setName("化学");
        List<Course> courseList2 = new ArrayList<>();
        courseList2.add(course1);
        courseList2.add(course3);
        student2.setCourseList(courseList2);
        studentList.add(student2);

        Observable.from(studentList)
                .map(new Func1<Student, Student>() {
                    @Override
                    public Student call(Student student) {
                        student.setName("haha");
                        return student;
                    }
                })

                .doOnNext(new Action1<Student>() {
                    @Override
                    public void call(Student student) {
                        student.setName("wawa");
                    }
                })
                .subscribe(new Observer<Student>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Student o) {
                        Log.e("Kill", o.getName());
                    }
                });

        Observable.from(studentList)
                .flatMap(new Func1<Student, Observable<Course>>() {
                    @Override
                    public Observable<Course> call(Student Student) {
                        Log.e("Kill2", Student.getName());
                        return Observable.from(Student.getCourseList());
                    }
                })
                .subscribe(new Observer<Course>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Course o) {
                        Log.e("Kill2", o.getName());
                    }
                });
    }


}
