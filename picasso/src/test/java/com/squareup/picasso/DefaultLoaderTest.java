package com.squareup.picasso;

import android.app.Activity;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;
import java.io.IOException;
import java.net.HttpURLConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static android.os.Build.VERSION_CODES.GINGERBREAD;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR2;
import static com.squareup.picasso.DefaultLoader.RESPONSE_SOURCE;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(PicassoTestRunner.class)
public class DefaultLoaderTest {
  private MockWebServer server;
  private DefaultLoader loader;

  @Before public void setUp() throws Exception {
    server = new MockWebServer();
    server.play();

    loader = new DefaultLoader(new Activity()) {
      @Override protected HttpURLConnection openConnection(String path) throws IOException {
        return (HttpURLConnection) server.getUrl(path).openConnection();
      }
    };
  }

  @After public void tearDown() throws Exception {
    server.shutdown();
  }

  @Ignore // Needs next Robolectric release for proper HttpResponseCache support.
  @Config(reportSdk = HONEYCOMB_MR2)
  @Test public void cacheOnlyInstalledOnce() throws Exception {
    DefaultLoader.cache = null;

    server.enqueue(new MockResponse());
    loader.load("/", false);
    Object cache = DefaultLoader.cache;
    assertThat(cache).isNotNull();

    server.enqueue(new MockResponse());
    loader.load("/", false);
    assertThat(DefaultLoader.cache).isSameAs(cache);
  }

  @Config(reportSdk = GINGERBREAD)
  @Test public void cacheOnlyInstalledOnApi13() throws Exception {
    DefaultLoader.cache = null;

    server.enqueue(new MockResponse());
    loader.load("/", false);
    Object cache = DefaultLoader.cache;
    assertThat(cache).isNull();
  }

  @Config(reportSdk = GINGERBREAD)
  @Test public void allowExpiredSetsCacheControl() throws Exception {
    server.enqueue(new MockResponse());
    loader.load("/", false);
    RecordedRequest request1 = server.takeRequest();
    assertThat(request1.getHeader("Cache-Control")).isNull();

    server.enqueue(new MockResponse());
    loader.load("/", true);
    RecordedRequest request2 = server.takeRequest();
    assertThat(request2.getHeader("Cache-Control")).isEqualTo("only-if-cached");
  }

  @Config(reportSdk = GINGERBREAD)
  @Test public void responseSourceHeaderSetsResponseValue() throws Exception {
    server.enqueue(new MockResponse());
    Loader.Response response1 = loader.load("/", false);
    assertThat(response1.cached).isFalse();

    server.enqueue(new MockResponse().addHeader(RESPONSE_SOURCE, "CACHE 200"));
    Loader.Response response2 = loader.load("/", true);
    assertThat(response2.cached).isTrue();
  }
}
