package com.github.portfolio.heyapp.Notifications;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAATUU5eDo:APA91bHeD-4nhz78aIrEXIwMlV0gJUwKcxtvQLIVYdDEqyMKwSlq7sEGYEdFtSyBhOnOsUHD1ujC0-olkgLb1WrfNeCNpwnprO5I20feW3s24Jcc0BMsZgs2zEB_ZxoBHg2SGgHu9Ph6"
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body NotificationSender body);
}
