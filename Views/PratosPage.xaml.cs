using CadernoReceitas.ViewModels;

namespace CadernoReceitas.Views;

public partial class PratosPage : ContentPage
{
    private readonly PratosViewModel viewModel;

    public PratosPage(PratosViewModel viewModel)
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
