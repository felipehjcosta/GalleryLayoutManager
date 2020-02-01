# GalleryLayoutManager

[![Build Status](https://travis-ci.org/felipehjcosta/GalleryLayoutManager.svg?branch=master)](https://travis-ci.org/felipehjcosta/GalleryLayoutManager)
[![Bintray](https://img.shields.io/bintray/v/fcostaa/maven/gallerylayoutmanager.svg)](https://bintray.com/fcostaa/maven/gallerylayoutmanager)

A custom LayoutManager to build a Gallery or a ViewPager like RecyclerView that shows items in a center-locked and support both HORIZONTAL and VERTICAL scroll. And View Recycle Machine is also supported. This library is a modified fork from [here](https://github.com/BCsl/GalleryLayoutManager).

## Screenshots

![ViewPager](./screenshots/ViewPager.gif)

![Demo](./screenshots/demo.gif)

## Usage

### Download

#### Gradle

```groovy
implementation 'com.github.felipehjcosta:gallerylayoutmanager:{lastest-release-version}'
```

### 2、In your code

#### Basis Usage

Use `GalleryLayoutManager#attach(RecycleView recycleView)` to setup `GalleryLayoutManager` for your `RecycleView` instead of `RecycleView#setLayoutManager(LayoutManager manager)`

```java
GalleryLayoutManager layoutManager = new GalleryLayoutManager(GalleryLayoutManager.HORIZONTAL);
//layoutManager.attach(mPagerRecycleView);  default selected position is 0
layoutManager.attach(mPagerRecycleView, 30);

//...
//setup adapter for your RecycleView
mPagerRecycleView.setAdapter(imageAdapter);
```

#### Listen to selection change

```java
layoutManager.setCallbackInFling(true); //should receive callback when flinging, default is false
layoutManager.setOnItemSelectedListener(new GalleryLayoutManager.OnItemSelectedListener() {
    @Override
    public void onItemSelected(RecyclerView recyclerView, View item, int position) {
        //.....
    }
});
```

#### Apply ItemTransformer just like ViewPager

Implements your `ItemTransformer`

```java
public class ScaleTransformer implements GalleryLayoutManager.ItemTransformer {

    @Override
    public void transformItem(GalleryLayoutManager layoutManager, View item,  int viewPosition, float fraction) {
        item.setPivotX(item.getWidth() / 2.f);
        item.setPivotY(item.getHeight()/2.0f);
        float scale = 1 - 0.3f * Math.abs(fraction);
        item.setScaleX(scale);
        item.setScaleY(scale);
    }
}
```

```java
// Apply ItemTransformer just like ViewPager
layoutManager.setItemTransformer(new ScaleTransformer());
```

License
-------

  MIT License
  
  Copyright (c) 2016 Felipe Costa
  
  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:
  
  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.
  
  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
