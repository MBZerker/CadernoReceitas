using CadernoReceitas.ViewModels;

namespace CadernoReceitas.Views;

public partial class ReceitasPage : ContentPage
{
    private readonly ReceitasViewModel viewModel;

    public ReceitasPage(ReceitasViewModel viewModel)
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
