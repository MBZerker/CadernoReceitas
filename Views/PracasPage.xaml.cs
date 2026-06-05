using CadernoReceitas.ViewModels;

namespace CadernoReceitas.Views;

public partial class PracasPage : ContentPage
{
    private readonly PracasViewModel viewModel;

    public PracasPage(PracasViewModel viewModel)
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
