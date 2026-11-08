using System.Security.Claims;
using ChannelMediator;
namespace FamilyFit.Contracts.Models.Exercises;

public sealed record ExerciseInput(string Id, string Name, string EquipmentId, string Muscle, string TargetType, string Instructions, string PostureTip);
public sealed record CreateExerciseRequest(ClaimsPrincipal User, ExerciseInput Input)
    : IRequest<Outcome<Exercise>>, IAuthorizedRequest, IChangesFamily { public bool AdminOnly => true; }
