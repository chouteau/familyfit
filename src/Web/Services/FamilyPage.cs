using System.Security.Claims;
using System.Globalization;
using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Contracts.Models.Family;
using FamilyFit.Core.Services;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;

namespace FamilyFit.Web.Services;

public abstract class FamilyPage : ComponentBase, IDisposable
{
    [Inject] protected IMediator Mediator { get; set; } = default!;
    [Inject] protected AuthenticationStateProvider Authentication { get; set; } = default!;
    [Inject] protected FamilyUpdates Updates { get; set; } = default!;
    [Inject] protected NavigationManager Navigation { get; set; } = default!;
    [Inject] protected TimeProvider Clock { get; set; } = default!;
    protected ClaimsPrincipal User = new();
    protected Actor Actor = default!;
    protected DashboardData Data = new([], [], [], [], [], []);
    protected string? Error;
    protected string? Message;
    protected bool Loading = true;
    protected bool Busy;
    protected DateOnly Today => FamilyCalendar.Today(Clock);
    protected DateOnly WeekStart;
    protected static readonly CultureInfo French = CultureInfo.GetCultureInfo("fr-FR");
    private readonly CancellationTokenSource _lifetime = new();
    protected CancellationToken Cancellation => _lifetime.Token;

    protected override async Task OnInitializedAsync()
    {
        WeekStart = FamilyCalendar.Monday(Today);
        User = (await Authentication.GetAuthenticationStateAsync()).User;
        var actor = await Mediator.GetActor(User, cancellationToken: Cancellation);
        if (!actor.Success) { Navigation.NavigateTo("/login", true); return; }
        Actor = actor.Value!;
        await Reload();
        Updates.Changed += OnChanged;
        _ = RefreshPeriodically();
    }
    private async Task RefreshPeriodically()
    {
        try { using var timer = new PeriodicTimer(TimeSpan.FromSeconds(30)); while (await timer.WaitForNextTickAsync(Cancellation)) await InvokeAsync(async () => { await Reload(); StateHasChanged(); }); }
        catch (OperationCanceledException) { }
    }
    private void OnChanged() { if (!_lifetime.IsCancellationRequested) _ = InvokeAsync(async () => { await Reload(); StateHasChanged(); }); }
    protected async Task Reload()
    {
        var result = await Mediator.GetDashboard(User, Cancellation);
        if (result.Success) Data = result.Value!;
        else if (result.Status == 401) Navigation.NavigateTo("/login", true);
        else Error = result.Error;
        Loading = false;
    }
    protected async Task<bool> Accept<T>(Outcome<T> outcome, string success)
    {
        Error = outcome.Error; Message = outcome.Success ? success : null;
        if (outcome.Success) await Reload();
        return outcome.Success;
    }
    protected ProgramDay? DayFor(Guid member, DateOnly date) => Data.Programs.Where(x => x.MemberId == member && x.State == "published").OrderByDescending(x => x.Version).SelectMany(x => x.Days).FirstOrDefault(x => x.Date == date);
    protected string StatusFor(ProgramDay? day)
    {
        if (day == null) return "unplanned";
        if (day.IsRest) return "rest";
        var results = Data.Results.Where(x => x.ProgramDayId == day.Id).ToList();
        if (results.Any(x => x.Status == "completed")) return "completed";
        if (results.Any(x => x.Status == "partial")) return "partial";
        if (results.Count != 0) return "abandoned";
        return day.Date < Today ? "missed" : day.Date == Today ? "todo" : "upcoming";
    }
    protected string LocationName(ProgramDay? day) => Data.Locations.FirstOrDefault(x => x.Id == day?.LocationId)?.Name ?? "Lieu à préciser";
    public virtual void Dispose() { Updates.Changed -= OnChanged; _lifetime.Cancel(); _lifetime.Dispose(); }
}
