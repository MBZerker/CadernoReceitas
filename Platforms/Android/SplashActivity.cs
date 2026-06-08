using Android.App;
using Android.Content;
using Android.Content.PM;
using Android.OS;
using Android.Widget;

namespace CadernoReceitas;

[Activity(Theme = "@android:style/Theme.Material.NoActionBar", MainLauncher = true, NoHistory = true, ScreenOrientation = ScreenOrientation.Portrait, ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation | ConfigChanges.ScreenLayout | ConfigChanges.SmallestScreenSize | ConfigChanges.Density)]
public sealed class SplashActivity : Activity
{
    protected override async void OnCreate(Bundle? savedInstanceState)
    {
        base.OnCreate(savedInstanceState);

        var image = new ImageView(this)
        {
            LayoutParameters = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MatchParent, LinearLayout.LayoutParams.MatchParent)
        };
        image.SetScaleType(ImageView.ScaleType.CenterCrop);
        image.SetImageResource(Resource.Drawable.splash_full);
        SetContentView(image);

        await Task.Delay(1800);
        StartActivity(new Intent(this, typeof(MainActivity)));
        Finish();
    }
}
