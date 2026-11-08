using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Workouts;

public sealed record SubmitWorkoutRequest(ClaimsPrincipal User, WorkoutSubmission Input) : IRequest<Outcome<WorkoutResultView>>, IAuthorizedRequest, IChangesFamily { public bool AdminOnly => false; }
