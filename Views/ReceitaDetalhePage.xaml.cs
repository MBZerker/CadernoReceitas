using CadernoReceitas.ViewModels;

namespace CadernoReceitas.Views;

public partial class ReceitaDetalhePage : ContentPage
{
    private readonly ReceitaDetalheViewModel viewModel;

    public ReceitaDetalhePage(ReceitaDetalheViewModel viewModel)
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
