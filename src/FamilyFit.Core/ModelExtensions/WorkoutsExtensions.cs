using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Workouts;

public static class WorkoutsExtensions
{
    public static Task<Outcome<WorkoutResultView>> SubmitWorkout(this IMediator mediator, ClaimsPrincipal User, WorkoutSubmission Input, CancellationToken cancellationToken = default) => mediator.Send(new SubmitWorkoutRequest(User, Input), cancellationToken);
    public static Task<Outcome<SyncSnapshot>> GetSync(this IMediator mediator, ClaimsPrincipal User, CancellationToken cancellationToken = default) => mediator.Send(new GetSyncRequest(User), cancellationToken);
}
