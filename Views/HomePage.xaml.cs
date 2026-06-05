using CadernoReceitas.ViewModels;

namespace CadernoReceitas.Views;

public partial class HomePage : ContentPage
{
    private readonly HomeViewModel viewModel;

    public HomePage(HomeViewModel viewModel)
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
