using CadernoReceitas.ViewModels;

namespace CadernoReceitas.Views;

public partial class CadernoPage : ContentPage
{
    private readonly CadernoViewModel viewModel;

    public CadernoPage(CadernoViewModel viewModel)
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
