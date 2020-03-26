package com.eso.socialmedia.notification;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAMVNs_pQ:APA91bGEU4Dadr9NR0RVq9QqKW-NBvr5lJ9JR6EkMH8jaz_uXusNxCaYQyrRVDMalJWHl8FLylv6XPS4uOsIGj-FOqlFHHK839GcgUnqEHTTJ0Mc6oHU9VJp7sOxDKq0ZWVbT_am-2fa"
    })

    @POST("fcm/send")
    Call<Response> sendNotification(@Body Sender body);
}
