namespace FamilyFit.Core.Services;

public sealed class FamilyUpdates
{
    public event Action? Changed;
    public void Notify() => Changed?.Invoke();
}
