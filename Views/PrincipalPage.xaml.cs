using CadernoReceitas.ViewModels;

namespace CadernoReceitas.Views;

public partial class PrincipalPage : ContentPage
{
    private readonly PrincipalViewModel viewModel;

    public PrincipalPage(PrincipalViewModel viewModel)
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
