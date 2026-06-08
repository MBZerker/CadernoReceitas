using CadernoReceitas.ViewModels;

namespace CadernoReceitas.Views;

public partial class GrupoPage : ContentPage
{
    private readonly GrupoViewModel viewModel;

    public GrupoPage(GrupoViewModel viewModel)
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
