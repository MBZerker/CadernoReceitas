using CadernoReceitas.ViewModels;

namespace CadernoReceitas.Views;

public partial class RestaurantesPage : ContentPage
{
    private readonly RestaurantesViewModel viewModel;

    public RestaurantesPage(RestaurantesViewModel viewModel)
    {
        InitializeComponent();
        BindingContext = this.viewModel = viewModel;
    }

    protected override async void OnAppearing()
    {
        base.OnAppearing();
        await viewModel.LoadCommand.ExecuteAsync(null);
    }
}
